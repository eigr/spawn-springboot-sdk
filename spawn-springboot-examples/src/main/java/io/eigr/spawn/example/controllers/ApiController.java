package io.eigr.spawn.example.controllers;

import io.eigr.spawn.example.actors.AbstractActor;
import io.eigr.spawn.example.actors.Sum;
import io.eigr.spawn.example.dto.ComputeRequestDTO;
import io.eigr.spawn.example.dto.ComputeResultDTO;
import io.eigr.spawn.springboot.starter.ActionRequest;
import io.eigr.spawn.springboot.starter.ActionResponse;
import io.eigr.spawn.springboot.starter.SpawnSystem;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Log4j2
@RestController
@RequestMapping("/api/v1")
public class ApiController {

    @Autowired
    private SpawnSystem actorSystem;

    @PostMapping("/actors/{name}")
    private Mono<?> spawnActor(@PathVariable String name) throws Exception {
        actorSystem.spawn(name, AbstractActor.class);
        return Mono.empty();
    }

    @PostMapping(
            value = "/actors/{name}/{action}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    private Mono<ComputeResultDTO> invokeActor(
            @PathVariable String name, @PathVariable String action, @RequestBody ComputeRequestDTO computeRequest) {
        Sum internalBusinessMessage = Sum.newBuilder()
                .setValue(computeRequest.getNumber())
                .build();

        ActionRequest request = ActionRequest.of()
                .actorName(name)
                .action(action)
                .value(internalBusinessMessage)
                .responseType(Sum.class)
                .build();

        return Mono.just(request).map(this::invoke);
    }

    private ComputeResultDTO invoke(ActionRequest req) {
        try {
            ActionResponse response = actorSystem.invoke(req);

            if (response.getValue().isPresent()) {
                Sum businessMessage = (Sum) response.getValue().get();

                return new ComputeResultDTO(businessMessage.getValue());
            }
        } catch (Exception e) {
            log.error("Error on compute number. Ignore results", e);
            return new ComputeResultDTO(0);
        }

        return new ComputeResultDTO(0);
    }
}
