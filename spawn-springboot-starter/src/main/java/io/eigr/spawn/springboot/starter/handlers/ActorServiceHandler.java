package io.eigr.spawn.springboot.starter.handlers;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.starter.Value;
import io.eigr.spawn.springboot.starter.annotations.ActorHandler;
import io.eigr.spawn.springboot.starter.internal.SpawnActorController;
import io.eigr.spawn.springboot.starter.workflows.Broadcast;
import io.eigr.spawn.springboot.starter.workflows.Forward;
import io.eigr.spawn.springboot.starter.workflows.Pipe;
import io.eigr.spawn.springboot.starter.workflows.SideEffect;
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

import java.util.List;
import java.util.stream.Collectors;

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
        log.info("Received Actor action request: {}", data);
        Protocol.ActorInvocation actorInvocationRequest = Protocol.ActorInvocation.parseFrom(data);
        Protocol.Context context = actorInvocationRequest.getCurrentContext();

        ActorOuterClass.ActorId actorId = actorInvocationRequest.getActor();
        String actor = actorId.getName();
        String system = actorId.getSystem();
        String commandName = actorInvocationRequest.getCommandName();

        Any value = actorInvocationRequest.getValue();

        Value valueResponse = actorController.callAction(system, actor, commandName, value, context);
        //valueResponse.getType();
        log.info("Actor {} received Action invocation for command {}. Result value: {}", actor, commandName, valueResponse);
        Any encodedState = Any.pack(valueResponse.getState());
        Any encodedValue = Any.pack(valueResponse.getValue());

        Protocol.Context updatedContext = Protocol.Context.newBuilder()
                .setState(encodedState)
                .build();

        Protocol.ActorInvocationResponse response = Protocol.ActorInvocationResponse.newBuilder()
                .setActorName(actor)
                .setActorSystem(system)
                .setValue(encodedValue)
                .setWorkflow(buildWorkflow(valueResponse))
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
}
