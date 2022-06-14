package io.eigr.spawn.springboot.starter.internal;

import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpawnActorController {

    private final List<Entity> entities;

    private final ApplicationContext context;

    private final SpawnProperties spawnProperties;

    private final SpawnClient client;

    private final ActorClassGraphEntityScan actorClassGraphEntityScan;

    public SpawnActorController(ApplicationContext context, SpawnClient client, SpawnProperties spawnProperties, ActorClassGraphEntityScan actorClassGraphEntityScan) {
        this.context = context;
        this.client = client;
        this.spawnProperties = spawnProperties;
        this.actorClassGraphEntityScan = actorClassGraphEntityScan;
        this.entities = actorClassGraphEntityScan.findEntities();
    }

    public void register() throws Exception {
        Map<String, ActorOuterClass.Actor> actors = entities.stream().map(actor -> {
            return ActorOuterClass.Actor.newBuilder()
                    .build();

        }).collect(Collectors.toMap(ActorOuterClass.Actor::getName, Function.identity()));

        ActorOuterClass.Registry registry = ActorOuterClass.Registry.newBuilder()
                .putAllActors(actors)
                .build();

        ActorOuterClass.ActorSystem actorSystem = ActorOuterClass.ActorSystem.newBuilder()
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

}
