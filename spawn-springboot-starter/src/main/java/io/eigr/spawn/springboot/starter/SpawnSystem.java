package io.eigr.spawn.springboot.starter;

import io.eigr.spawn.springboot.internal.SpawnActorController;

import java.util.Objects;
import java.util.Optional;

public class SpawnSystem {

    private final SpawnActorController actorController;

    public SpawnSystem(SpawnActorController actorController) {
        this.actorController = actorController;
    }

    public void registerAllActors() throws Exception {
        actorController.register();
    }

    public void spawn(String name, Class actor) throws Exception {
        actorController.spawn(name, actor);
    }

    public ActionResponse invoke(ActionRequest<?,?> req) throws Exception {
        ActionResponse.ActionResponseBuilder responseBuilder = ActionResponse.builder();

        if (req.getValue().isPresent() && Objects.nonNull(req.getActorName())) {
            Object resp = actorController.invoke(
                    req.getActorName(),
                    req.getAction(),
                    req.getValue().get(),
                    req.getResponseType(),
                    req.getOpts());

            return responseBuilder
                    .value(Optional.of(resp))
                    .build();
        }

        if (req.getValue().isPresent() && Objects.nonNull(req.getActorType())) {
            Object resp = actorController.invoke(
                    req.getActorType().getSimpleName(),
                    req.getAction(),
                    req.getValue().get(),
                    req.getResponseType(),
                    req.getOpts());

            return responseBuilder
                    .value(Optional.of(resp))
                    .build();
        }

        if (!req.getValue().isPresent() && Objects.nonNull(req.getActorName())) {
            Object resp = actorController.invoke(
                    req.getActorName(),
                    req.getAction(),
                    req.getResponseType(),
                    req.getOpts());

            return responseBuilder
                    .value(Optional.of(resp))
                    .build();
        }

        if (!req.getValue().isPresent() && Objects.nonNull(req.getActorType())) {
            Object resp = actorController.invoke(
                    req.getActorType().getSimpleName(),
                    req.getAction(),
                    req.getResponseType(),
                    req.getOpts());

            return responseBuilder
                    .value(Optional.of(resp))
                    .build();
        }

        return responseBuilder
                .value(Optional.empty())
                .build();
    }

}
