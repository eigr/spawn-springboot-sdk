package io.eigr.spawn.springboot.starter.internal;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import io.eigr.spawn.springboot.starter.exceptions.ActorInvokeException;
import io.eigr.spawn.springboot.starter.exceptions.ActorNotFoundException;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpawnActorController {

    private final List<Entity> entities;

    private final Map<String, ActorOuterClass.Actor> actors;

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
        ActorOuterClass.Registry registry = ActorOuterClass.Registry.newBuilder()
                .putAllActors(actors)
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

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String actor, String cmd, Class<T> outputType) throws Exception {
        return invokeActor(actor, cmd, Empty.getDefaultInstance(), outputType);
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String actor, String cmd, S value, Class<T> outputType) throws Exception {
        return invokeActor(actor, cmd, value, outputType);
    }

    public Value callAction(String system, String actor, String commandName, Any value, Protocol.Context context) {
        if (actors.containsKey(actor)) {
            Optional<Entity> optionalEntity = getEntityByActor(actor);
            if (optionalEntity.isPresent()) {
                try {
                    Entity entity = optionalEntity.get();
                    final Object actorRef = this.context.getBean(entity.getActorType());
                    final Entity.EntityMethod entityMethod = entity.getCommands().get(commandName);
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
        } else {
            System.out.println("Nao entrou na condicao");
        }
        return null;
    }

    private <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invokeActor(String actor, String cmd, S value, Class<T> outputType) throws Exception {
        Objects.requireNonNull(actorSystem, "ActorSystem not initialized!");

        final ActorOuterClass.Actor actorRef = ActorOuterClass.Actor.newBuilder()
                .setName(actor)
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
                return outputType.cast(resp.getValue().unpack(outputType));
        }

        throw new ActorNotFoundException();
    }

    @NotNull
    private Map<String, ActorOuterClass.Actor> getActors(List<Entity> entities) {
        return entities.stream().map(actor -> {

            ActorOuterClass.ActorSnapshotStrategy snapshotStrategy = ActorOuterClass.ActorSnapshotStrategy.newBuilder()
                    .setTimeout(ActorOuterClass.TimeoutStrategy.newBuilder().setTimeout(actor.getSnapshotTimeout()).build())
                    .build();

            ActorOuterClass.ActorDeactivateStrategy deactivateStrategy = ActorOuterClass.ActorDeactivateStrategy.newBuilder()
                    .setTimeout(ActorOuterClass.TimeoutStrategy.newBuilder().setTimeout(actor.getDeactivateTimeout()).build())
                    .build();

            return ActorOuterClass.Actor.newBuilder()
                    .setName(actor.getActorName())
                    .setPersistent(actor.isPersistent())
                    .setSnapshotStrategy(snapshotStrategy)
                    .setDeactivateStrategy(deactivateStrategy)
                    .setState(ActorOuterClass.ActorState.newBuilder().build()) // Init with empty state
                    .build();

        }).collect(Collectors.toMap(ActorOuterClass.Actor::getName, Function.identity()));
    }

    private Optional<Entity> getEntityByActor(String actor) {
        return entities.stream().filter(e -> e.getActorName().equalsIgnoreCase(actor)).findFirst();
    }
}
