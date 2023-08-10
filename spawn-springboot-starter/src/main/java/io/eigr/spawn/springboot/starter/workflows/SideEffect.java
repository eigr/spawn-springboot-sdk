package io.eigr.spawn.springboot.starter.workflows;

import com.google.protobuf.Any;
import com.google.protobuf.GeneratedMessageV3;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.internal.GlobalEnvironment;
import io.eigr.spawn.springboot.starter.InvocationOpts;
import org.springframework.core.env.Environment;

import java.util.Optional;

public final class SideEffect<T extends GeneratedMessageV3> {

    private final String actor;
    private final String command;
    private final T payload;

    private final Environment env;

    private final Optional<InvocationOpts> opts;

    private SideEffect(String actor, String command, T payload) {
        this.actor = actor;
        this.command = command;
        this.payload = payload;
        this.opts = Optional.empty();
        this.env = GlobalEnvironment.getEnvironment();
    }

    private SideEffect(String actor, String command, T payload, InvocationOpts opts) {
        this.actor = actor;
        this.command = command;
        this.payload = payload;
        this.opts = Optional.of(opts);
        this.env = GlobalEnvironment.getEnvironment();
    }

    public SideEffect to(String actor, String command, T payload) {
        return new SideEffect(actor, command, payload);
    }

    public SideEffect to(String actor, String command, T payload, InvocationOpts opts) {
        return new SideEffect(actor, command, payload, opts);
    }

    public Protocol.SideEffect build() {
        String system = this.env.getProperty("io.eigr.spawn.springboot.starter.actorSystem");
        Protocol.InvocationRequest.Builder requestBuilder = Protocol.InvocationRequest.newBuilder();

        if (this.opts.isPresent()) {
            InvocationOpts options = this.opts.get();
            if (options.getDelay().isPresent() && !options.getScheduledTo().isPresent()) {
                requestBuilder.setScheduledTo(options.getDelay().get());
            } else if (options.getScheduledTo().isPresent()){
                requestBuilder.setScheduledTo(options.getScheduleTimeInLong());
            }
        }

        requestBuilder.setSystem(ActorOuterClass.ActorSystem.newBuilder()
                        .setName(system)
                        .build())
                .setActor(ActorOuterClass.Actor.newBuilder()
                        .setId(ActorOuterClass.ActorId.newBuilder()
                                .setSystem(system)
                                .setName(this.actor)
                                .build())
                        .build())
                .setAsync(true)
                .setActionName(command)
                .setValue(Any.pack(payload));

        return Protocol.SideEffect.newBuilder()
                .setRequest(requestBuilder.build())
                .build();
    }
}
