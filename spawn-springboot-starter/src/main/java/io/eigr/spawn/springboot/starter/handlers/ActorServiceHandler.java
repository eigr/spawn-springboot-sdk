package io.eigr.spawn.springboot.starter.handlers;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.springboot.starter.Value;
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
        log.debug("Received Actor action request: {}", data);
        Protocol.ActorInvocation actorInvocationRequest = Protocol.ActorInvocation.parseFrom(data);
        Protocol.Context context = actorInvocationRequest.getCurrentContext();

        String actor = actorInvocationRequest.getActorName();
        String system = actorInvocationRequest.getActorSystem();
        String commandName = actorInvocationRequest.getCommandName();

        Any value = actorInvocationRequest.getValue();

        Value valueResponse = actorController.callAction(system, actor, commandName, value, context);
        //valueResponse.getType();
        log.info("Actor {} received Action invocation for command {}. Result value: {}", actor, commandName, valueResponse);
        Any encodedState = Any.pack(valueResponse.getState());
        Any encodedValue = Any.pack(valueResponse.getState());

        Protocol.Context updatedContext = Protocol.Context.newBuilder()
                .setState(encodedState)
                .build();

        Protocol.ActorInvocationResponse response = Protocol.ActorInvocationResponse.newBuilder()
                .setActorName(actor)
                .setActorSystem(system)
                .setValue(encodedValue)
                .setUpdatedContext(updatedContext)
                .build();

        byte[] responseBytes = response.toByteArray();
        log.debug("Response raw bytes: {}", responseBytes);
        ByteArrayResource resource = new ByteArrayResource(responseBytes);
        long length = resource.contentLength();
        log.debug("Content length for ActorInvocationResponse: {}",length );

        return Mono.just(ResponseEntity
                .ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(length)
                .body(resource));
    }

}
