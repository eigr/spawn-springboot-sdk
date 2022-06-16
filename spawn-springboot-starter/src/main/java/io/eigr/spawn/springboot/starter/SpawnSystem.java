package io.eigr.spawn.springboot.starter;

import com.google.protobuf.GeneratedMessageV3;
import io.eigr.spawn.springboot.starter.internal.SpawnActorController;

public class SpawnSystem {

    private final SpawnActorController actorController;

    public SpawnSystem(SpawnActorController actorController) {
        this.actorController = actorController;
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(String actor, String cmd, S value, Class<T> outputType) throws Exception {
        return actorController.invoke(actor, cmd, value, outputType);
    }

    public <T extends GeneratedMessageV3, S extends GeneratedMessageV3> Object invoke(Class actor, String cmd, S value, Class<T> outputType) throws Exception {
        return actorController.invoke(actor.getSimpleName(), cmd, value, outputType);
    }
}
