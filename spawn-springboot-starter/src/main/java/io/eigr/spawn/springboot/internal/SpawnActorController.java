package io.eigr.spawn.springboot.internal;

import com.google.protobuf.Any;
import com.google.protobuf.Empty;
import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.internal.exceptions.ActorInvokeException;
import io.eigr.spawn.springboot.starter.ActorContext;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import io.eigr.spawn.springboot.internal.exceptions.ActorNotFoundException;
import io.eigr.spawn.springboot.starter.workflows.Broadcast;
import io.eigr.spawn.springboot.starter.workflows.Forward;
import io.eigr.spawn.springboot.starter.workflows.Pipe;
import io.eigr.spawn.springboot.starter.workflows.SideEffect;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class SpawnActorController {
    private static final Logger log = LoggerFactory.getLogger(SpawnActorController.class);

    private final Map<String, ActorOuterClass.Actor> actors;
    private final ActorClassGraphEntityScan actorClassGraphEntityScan;
    private final ApplicationContext context;
    private final List<Entity> entities;
    private final SpawnClient spawnClient;
    private final SpawnProperties spawnProperties;
    private ActorOuterClass.ActorSystem actorSystem;


    public SpawnActorController(ApplicationContext context, SpawnClient spawnClient, SpawnProperties spawnProperties, ActorClassGraphEntityScan actorClassGraphEntityScan) {
        this.context = context;
        this.spawnClient = spawnClient;
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

        spawnClient.register(registration);
    }

    public void spawn(String name, Class actorClass) throws Exception {
        Optional<Entity> actorEntity = actorClassGraphEntityScan.findEntity(actorClass);

        if (actorEntity.isPresent()) {
            Entity entity = actorEntity.get();
            String originalName = entity.getActorName();
            entity.setActorName(name);

            ActorOuterClass.Actor actor = getActor(entity, originalName);
            log.trace("Spawning Actor {}", actor);
            if (!actors.containsKey(name)) {
                actors.put(name, actor);
                this.entities.add(entity);
            }

            Protocol.SpawnRequest registration = Protocol.SpawnRequest.newBuilder()
                    .addActors(actor.getId())
                    .build();

            spawnClient.spawn(registration);
        }
    }

    public Protocol.ActorInvocationResponse handleRequest(byte[] request) throws InvalidProtocolBufferException {
        Protocol.ActorInvocation actorInvocationRequest = Protocol.ActorInvocation.parseFrom(request);
        Protocol.Context context = actorInvocationRequest.getCurrentContext();

        ActorOuterClass.ActorId actorId = actorInvocationRequest.getActor();
        String actor = actorId.getName();
        String system = actorId.getSystem();
        String commandName = actorInvocationRequest.getCommandName();

        Any value = actorInvocationRequest.getValue();

        Optional<Value> maybeValueResponse = callAction(system, actor, commandName, value, context);
        log.info("Actor {} return ActorInvocationResponse for command {}. Result value: {}",
                actor, commandName, maybeValueResponse);

        if (maybeValueResponse.isPresent()) {
            Value valueResponse = maybeValueResponse.get();
            Any encodedState = Any.pack(valueResponse.getState());
            Any encodedValue = Any.pack(valueResponse.getResponse());

            Protocol.Context updatedContext = Protocol.Context.newBuilder()
                    .setState(encodedState)
                    .build();

            return Protocol.ActorInvocationResponse.newBuilder()
                    .setActorName(actor)
                    .setActorSystem(system)
                    .setValue(encodedValue)
                    .setWorkflow(buildWorkflow(valueResponse))
                    .setUpdatedContext(updatedContext)
                    .build();
        }

        throw new ActorInvokeException("Action result is null");
    }

    public <T extends GeneratedMessageV3> Object invoke(String actor, String cmd, Class<T> outputType) throws Exception {
        return invokeActor(actor, cmd, Empty.getDefaultInstance(), outputType);
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String actor, String cmd, S value, Class<T> outputType) throws Exception {
        return invokeActor(actor, cmd, value, outputType);
    }

    private <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invokeActor(String actor, String cmd, S argument, Class<T> outputType) throws Exception {
        Objects.requireNonNull(actorSystem, "ActorSystem not initialized!");

        final ActorOuterClass.Actor actorRef = ActorOuterClass.Actor.newBuilder()
                .setId(
                        ActorOuterClass.ActorId.newBuilder()
                                .setName(actor)
                                .build()
                )
                .build();

        Any commandArg = Any.pack(argument);

        Protocol.InvocationRequest invocationRequest = Protocol.InvocationRequest.newBuilder()
                .setAsync(false)
                .setSystem(actorSystem)
                .setActor(actorRef)
                .setCommandName(cmd)
                .setValue(commandArg)
                .build();

        Protocol.InvocationResponse resp = spawnClient.invoke(invocationRequest);
        final Protocol.RequestStatus status = resp.getStatus();
        switch (status.getStatus()) {
            case UNKNOWN:
            case ERROR:
            case UNRECOGNIZED:
                throw new ActorInvokeException(
                        String.format("Unknown error when trying to invoke Actor %s", actor));
            case ACTOR_NOT_FOUND:
                throw new ActorNotFoundException();
            case OK:
                if (resp.hasValue()) {
                    return outputType.cast(resp.getValue()
                            .unpack(outputType));
                }
                return null;
        }

        throw new ActorNotFoundException();
    }

    private Optional<Value> callAction(String system, String actor, String commandName, Any value, Protocol.Context context) {
        Optional<Entity> optionalEntity = getEntityByActor(actor);
        if (optionalEntity.isPresent()) {
            try {
                Entity entity = optionalEntity.get();
                final Object actorRef = this.context.getBean(entity.getActorType());
                final Entity.EntityMethod entityMethod = entity.getActions().get(commandName);
                final Method actorMethod = entityMethod.getMethod();
                Class inputType = entityMethod.getInputType();

                ActorContext actorContext;
                if (context.hasState()) {
                    Object state = context.getState().unpack(entity.getStateType());
                    actorContext = new ActorContext(state);
                } else {
                    actorContext = new ActorContext();
                }

                if (inputType.isAssignableFrom(ActorContext.class)) {
                    return Optional.of((Value) actorMethod.invoke(actorRef, actorContext));
                } else {
                    final Object unpack = value.unpack(entityMethod.getInputType());
                    return Optional.of((Value) actorMethod.invoke(actorRef, unpack, actorContext));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (InvalidProtocolBufferException e) {
                throw new RuntimeException(e);
            }
        }
        return Optional.empty();
    }

    private Protocol.Workflow buildWorkflow(Value valueResponse) {
        Protocol.Workflow.Builder workflowBuilder = Protocol.Workflow.newBuilder();

        if (valueResponse.getBroadcast().isPresent()) {
            Protocol.Broadcast b = ((Broadcast) valueResponse.getBroadcast().get()).build();
            workflowBuilder.setBroadcast(b);
        }

        if (valueResponse.getForward().isPresent()) {
            Protocol.Forward f = ((Forward) valueResponse.getForward().get()).build();
            workflowBuilder.setForward(f);
        }

        if (valueResponse.getPipe().isPresent()) {
            Protocol.Pipe p = ((Pipe) valueResponse.getPipe().get()).build();
            workflowBuilder.setPipe(p);
        }

        if (valueResponse.getEffects().isPresent()) {
            List<SideEffect> efs = ((List<SideEffect>) valueResponse.getEffects().get());
            workflowBuilder.addAllEffects(getProtocolEffects(efs));
        }

        return workflowBuilder.build();
    }

    private List<Protocol.SideEffect> getProtocolEffects(List<SideEffect> effects) {
        return effects.stream()
                .map(SideEffect::build)
                .collect(Collectors.toList());
    }

    @NotNull
    private Map<String, ActorOuterClass.Actor> getActors(List<Entity> entities) {
        return entities.stream().map(actorEntity -> {

            ActorOuterClass.ActorSnapshotStrategy snapshotStrategy =
                    ActorOuterClass.ActorSnapshotStrategy.newBuilder()
                            .setTimeout(
                                    ActorOuterClass.TimeoutStrategy.newBuilder()
                                            .setTimeout(actorEntity.getSnapshotTimeout())
                                            .build()
                            )
                            .build();

            ActorOuterClass.ActorDeactivationStrategy deactivateStrategy =
                    ActorOuterClass.ActorDeactivationStrategy.newBuilder()
                            .setTimeout(
                                    ActorOuterClass.TimeoutStrategy.newBuilder()
                                            .setTimeout(actorEntity.getDeactivateTimeout())
                                            .build()
                            )
                            .build();

            ActorOuterClass.ActorSettings settings = ActorOuterClass.ActorSettings.newBuilder()
                    .setKind(actorEntity.getKind())
                    .setStateful(actorEntity.isStateful())
                    .setSnapshotStrategy(snapshotStrategy)
                    .setDeactivationStrategy(deactivateStrategy)
                    .setMinPoolSize(actorEntity.getMinPoolSize())
                    .setMaxPoolSize(actorEntity.getMaxPoolSize())
                    .build();

            Map<String, String> tags = new HashMap<>();
            ActorOuterClass.Metadata metadata = ActorOuterClass.Metadata.newBuilder()
                    .setChannelGroup("") // TODO CORRECT SET HERE
                    .putAllTags(tags)
                    .build();

            return ActorOuterClass.Actor.newBuilder()
                    .setId(
                            ActorOuterClass.ActorId.newBuilder()
                                    .setName(actorEntity.getActorName())
                                    .setSystem(spawnProperties.getActorSystem())
                                    .build()
                    )
                    .setMetadata(metadata)
                    .setSettings(settings)
                    .addAllCommands(getCommands(actorEntity))
                    .addAllTimerCommands(getTimerCommands(actorEntity))
                    .setState(ActorOuterClass.ActorState.newBuilder().build()) // Init with empty state
                    .build();

        }).collect(Collectors.toMap(actor -> actor.getId().getName(), Function.identity()));
    }

    private ActorOuterClass.Actor getActor(Entity actorEntity, String originalName) {
        ActorOuterClass.ActorSnapshotStrategy snapshotStrategy =
                ActorOuterClass.ActorSnapshotStrategy.newBuilder()
                        .setTimeout(
                                ActorOuterClass.TimeoutStrategy.newBuilder()
                                        .setTimeout(actorEntity.getSnapshotTimeout())
                                        .build()
                        )
                        .build();

        ActorOuterClass.ActorDeactivationStrategy deactivateStrategy =
                ActorOuterClass.ActorDeactivationStrategy.newBuilder()
                        .setTimeout(
                                ActorOuterClass.TimeoutStrategy.newBuilder()
                                        .setTimeout(actorEntity.getDeactivateTimeout())
                                        .build()
                        )
                        .build();

        ActorOuterClass.ActorSettings settings = ActorOuterClass.ActorSettings.newBuilder()
                .setKind(actorEntity.getKind())
                .setStateful(actorEntity.isStateful())
                .setSnapshotStrategy(snapshotStrategy)
                .setDeactivationStrategy(deactivateStrategy)
                .setMinPoolSize(actorEntity.getMinPoolSize())
                .setMaxPoolSize(actorEntity.getMaxPoolSize())
                .build();

        return ActorOuterClass.Actor.newBuilder()
                .setId(
                        ActorOuterClass.ActorId.newBuilder()
                                .setName(actorEntity.getActorName())
                                .setParent(originalName)
                                .setSystem(spawnProperties.getActorSystem())
                                .build()
                )
                .setSettings(settings)
                .addAllCommands(getCommands(actorEntity))
                .addAllTimerCommands(getTimerCommands(actorEntity))
                .setState(ActorOuterClass.ActorState.newBuilder().build())
                .build();
    }

    private List<ActorOuterClass.Command> getCommands(Entity actorEntity) {
        return actorEntity.getActions()
                .values()
                .stream()
                .filter(v -> Entity.EntityMethodType.DIRECT.equals(v.getType()))
                .map(action ->
                        ActorOuterClass.Command.newBuilder()
                                .setName(action.getName())
                                .build()
                )
                .collect(Collectors.toList());
    }

    private List<ActorOuterClass.FixedTimerCommand> getTimerCommands(Entity actorEntity) {
        return actorEntity.getActions()
                .values()
                .stream()
                .filter(v -> Entity.EntityMethodType.TIMER.equals(v.getType()))
                .map(action ->
                        ActorOuterClass.FixedTimerCommand.newBuilder()
                                .setCommand(
                                        ActorOuterClass.Command.newBuilder()
                                                .setName(action.getName())
                                                .build())
                                .setSeconds(action.getFixedPeriod())
                                .build()
                )
                .collect(Collectors.toList());
    }

    private Optional<Entity> getEntityByActor(String actor) {
        return entities.stream()
                .filter(e -> e.getActorName().equalsIgnoreCase(actor))
                .findFirst();
    }
}
