package io.eigr.spawn.springboot.starter.handlers;

import com.google.protobuf.InvalidProtocolBufferException;
import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.springboot.starter.annotations.ActorHandler;
import io.eigr.spawn.springboot.starter.internal.SpawnActorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@ActorHandler
@RequestMapping("/api/v1/actors")
public final class ActorServiceHandler {
    private static final Logger log = LoggerFactory.getLogger(ActorServiceHandler.class);
    private final SpawnActorController actorController;
    @Autowired
    public ActorServiceHandler(SpawnActorController actorController){
        this.actorController = actorController;
    }
    @PostMapping(value = "/actions",
            consumes = {MediaType.APPLICATION_OCTET_STREAM_VALUE},
            produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE}
    )
    public Mono<ResponseEntity<ByteArrayResource>> post(@RequestBody() byte[] data) throws InvalidProtocolBufferException {
        log.trace("Received Actor Action Request: {}", data);
        Protocol.ActorInvocationResponse actorInvocationResponse = actorController.handleRequest(data);
        return buildHttpResponse(actorInvocationResponse);
    }

    private Mono<ResponseEntity<ByteArrayResource>> buildHttpResponse(Protocol.ActorInvocationResponse response) {
        byte[] responseBytes = response.toByteArray();
        ByteArrayResource resource = new ByteArrayResource(responseBytes);
        long length = resource.contentLength();

        log.trace("Response raw bytes: {}", responseBytes);
        log.trace("Content length for ActorInvocationResponse: {}",length );
        return Mono.just(ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(length)
                .body(resource));
    }
}
