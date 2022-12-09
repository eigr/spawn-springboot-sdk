package io.eigr.spawn.springboot.starter.internal;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.ActorIdentity;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import io.eigr.spawn.springboot.starter.exceptions.ActorInvokeException;
import io.eigr.spawn.springboot.starter.exceptions.ActorNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpawnActorController {

    private List<Entity> entities;

    private Map<String, ActorOuterClass.Actor> actors;

    private final ApplicationContext context;

    private final SpawnProperties spawnProperties;

    private final SpawnClient client;

    private final ActorClassGraphEntityScan actorClassGraphEntityScan;

    private ActorOuterClass.ActorSystem actorSystem;

    public SpawnActorController(ApplicationContext context, SpawnClient client, SpawnProperties spawnProperties, ActorClassGraphEntityScan actorClassGraphEntityScan) {
        this.context = context;
        this.client = client;
        this.spawnProperties = spawnProperties;
        this.actorClassGraphEntityScan = actorClassGraphEntityScan;
        this.entities = actorClassGraphEntityScan.findEntities();
        this.actors = getActors(entities);
    }

    public void register() throws Exception {
        Map<String, ActorOuterClass.Actor> realActors = new HashMap<>();
        for (Map.Entry<String, ActorOuterClass.Actor> actor : actors.entrySet()) {
            if (!actor.getValue().getId().getName().equalsIgnoreCase(ActorIdentity.Abstract)) {
                realActors.put(actor.getKey(), actor.getValue());
            }
        }

        ActorOuterClass.Registry registry = ActorOuterClass.Registry.newBuilder()
                .putAllActors(realActors)
                .build();

        actorSystem = ActorOuterClass.ActorSystem.newBuilder()
                .setName(spawnProperties.getActorSystem())
                .setRegistry(registry)
                .build();

        Protocol.ServiceInfo si = Protocol.ServiceInfo.newBuilder()
                .setServiceName("jvm-spring-boot-sdk")
                .setServiceVersion("0.1.1")
                .setServiceRuntime(System.getProperty("java.version"))
                .setProtocolMajorVersion(1)
                .setProtocolMinorVersion(1)
                .build();

        Protocol.RegistrationRequest registration = Protocol.RegistrationRequest.newBuilder()
                .setServiceInfo(si)
                .setActorSystem(actorSystem)
                .build();

        client.register(registration);
    }

    public void spawn(String name, Class actorClass) throws Exception {
        Map<String, ActorOuterClass.Actor> concreteActor = new HashMap<>();
        Optional<Entity> actorEntity = actorClassGraphEntityScan.findEntity(actorClass);

        if (actorEntity.isPresent()) {
            Entity entity = actorEntity.get();
            entity.setActorName(name);

            ActorOuterClass.Actor actor = getActor(entity);
            concreteActor.put(name, actor);

            if (!actors.containsKey(name)) {
                actors.put(name, actor);
                this.entities.add(entity);
            }

            List<ActorOuterClass.ActorId> ids = actors
                    .entrySet()
                    .stream()
                    .map(v -> v.getValue().getId()).collect(Collectors.toList());

            Protocol.SpawnRequest registration = Protocol.SpawnRequest.newBuilder()
                    .addAllActors(ids)
                    .build();

            client.spawn(registration);
        }
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String actor, String cmd, Class<T> outputType) throws Exception {
        return invokeActor(actor, cmd, Empty.getDefaultInstance(), outputType);
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String actor, String cmd, S value, Class<T> outputType) throws Exception {
        return invokeActor(actor, cmd, value, outputType);
    }

    public Value callAction(String system, String actor, String commandName, Any value, Protocol.Context context) {
            Optional<Entity> optionalEntity = getEntityByActor(actor);
            if (optionalEntity.isPresent()) {
                try {
                    Entity entity = optionalEntity.get();
                    final Object actorRef = this.context.getBean(entity.getActorType());
                    final Entity.EntityMethod entityMethod = entity.getActions().get(commandName);
                    final Method actorMethod = entityMethod.getMethod();
                    Class inputType = entityMethod.getInputType();

                    ActorContext actorContext;
                    if (context.hasState() && Objects.nonNull(context.getState())) {
                        Object state = context.getState().unpack(entity.getStateType());
                        actorContext = new ActorContext(state);
                    } else {
                        actorContext = new ActorContext();
                    }

                    if (inputType.isAssignableFrom(ActorContext.class)) {
                        return (Value) actorMethod.invoke(actorRef, actorContext);
                    } else {
                        final Object unpack = value.unpack(entityMethod.getInputType());
                        return (Value) actorMethod.invoke(actorRef, unpack, actorContext);
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                } catch (InvocationTargetException e) {
                    throw new RuntimeException(e);
                } catch (InvalidProtocolBufferException e) {
                    throw new RuntimeException(e);
                }
            }
        return null;
    }

    private <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invokeActor(String actor, String cmd, S value, Class<T> outputType) throws Exception {
        Objects.requireNonNull(actorSystem, "ActorSystem not initialized!");

        final ActorOuterClass.Actor actorRef = ActorOuterClass.Actor.newBuilder()
                .setId(
                        ActorOuterClass.ActorId.newBuilder()
                                .setName(actor)
                                .build()
                )
                .build();

        Any stateValue = Any.pack(value);

        Protocol.InvocationRequest invocationRequest = Protocol.InvocationRequest.newBuilder()
                .setAsync(false)
                .setSystem(actorSystem)
                .setActor(actorRef)
                .setCommandName(cmd)
                .setValue(stateValue)
                .build();

        Protocol.InvocationResponse resp = client.invoke(invocationRequest);
        final Protocol.RequestStatus status = resp.getStatus();
        switch (status.getStatus()) {
            case UNKNOWN:
            case ERROR:
            case UNRECOGNIZED:
                throw new ActorInvokeException();
            case ACTOR_NOT_FOUND:
                throw new ActorNotFoundException();
            case OK:
                if (resp.hasValue() && Objects.nonNull(resp.getValue())) {
                    return outputType.cast(resp.getValue().unpack(outputType));
                }
                return null;
        }

        throw new ActorNotFoundException();
    }

    @NotNull
    private Map<String, ActorOuterClass.Actor> getActors(List<Entity> entities) {
        return entities.stream().map(actor -> {

            ActorOuterClass.ActorSnapshotStrategy snapshotStrategy =
                    ActorOuterClass.ActorSnapshotStrategy.newBuilder()
                    .setTimeout(
                            ActorOuterClass.TimeoutStrategy.newBuilder()
                                    .setTimeout(actor.getSnapshotTimeout())
                                    .build()
                    )
                    .build();

            ActorOuterClass.ActorDeactivationStrategy deactivateStrategy =
                    ActorOuterClass.ActorDeactivationStrategy.newBuilder()
                    .setTimeout(
                            ActorOuterClass.TimeoutStrategy.newBuilder()
                                    .setTimeout(actor.getDeactivateTimeout())
                                    .build()
                    )
                    .build();

            ActorOuterClass.ActorSettings settings = ActorOuterClass.ActorSettings.newBuilder()
                    .setStateful(actor.isStateful())
                    .setSnapshotStrategy(snapshotStrategy)
                    .setDeactivationStrategy(deactivateStrategy)
                    .build();

            return ActorOuterClass.Actor.newBuilder()
                    .setId(
                            ActorOuterClass.ActorId.newBuilder()
                                    .setName(actor.getActorName())
                                    .build()
                    )
                    .setSettings(settings)
                    .setState(ActorOuterClass.ActorState.newBuilder().build()) // Init with empty state
                    .build();

        }).collect(Collectors.toMap(actor -> actor.getId().getName(), Function.identity()));
    }

    private ActorOuterClass.Actor getActor(Entity entity) {
        ActorOuterClass.ActorSnapshotStrategy snapshotStrategy =
                ActorOuterClass.ActorSnapshotStrategy.newBuilder()
                .setTimeout(
                        ActorOuterClass.TimeoutStrategy.newBuilder()
                                .setTimeout(entity.getSnapshotTimeout())
                                .build()
                )
                .build();

        ActorOuterClass.ActorDeactivationStrategy deactivateStrategy =
                ActorOuterClass.ActorDeactivationStrategy.newBuilder()
                .setTimeout(
                        ActorOuterClass.TimeoutStrategy.newBuilder()
                                .setTimeout(entity.getDeactivateTimeout())
                                .build()
                )
                .build();

        ActorOuterClass.ActorSettings settings = ActorOuterClass.ActorSettings.newBuilder()
                .setStateful(entity.isStateful())
                .setSnapshotStrategy(snapshotStrategy)
                .setDeactivationStrategy(deactivateStrategy)
                .build();

        return ActorOuterClass.Actor.newBuilder()
                .setId(
                        ActorOuterClass.ActorId.newBuilder()
                                .setName(entity.getActorName())
                                .build()
                )
                .setSettings(settings)
                .setState(ActorOuterClass.ActorState.newBuilder().build())
                .build();
    }

    private Optional<Entity> getEntityByActor(String actor) {
        return entities.stream()
                .filter(e -> e.getActorName().equalsIgnoreCase(actor))
                .findFirst();
    }
}
