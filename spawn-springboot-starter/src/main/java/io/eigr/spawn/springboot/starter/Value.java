package io.eigr.spawn.springboot.starter;

import com.google.protobuf.GeneratedMessageV3;
import io.eigr.spawn.springboot.starter.workflows.Broadcast;
import io.eigr.spawn.springboot.starter.workflows.Forward;
import io.eigr.spawn.springboot.starter.workflows.Pipe;
import io.eigr.spawn.springboot.starter.workflows.SideEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Value<S extends GeneratedMessageV3, R extends GeneratedMessageV3> {

    private final S state;
    private final R response;
    private final Optional<Broadcast<?>> broadcast;
    private final Optional<Forward> forward;
    private final Optional<Pipe> pipe;
    private final Optional<List<SideEffect>> effects;
    private final ResponseType type;

    public Value() {
        this.state = null;
        this.response = null;
        this.broadcast = Optional.empty();
        this.forward = Optional.empty();
        this.pipe = Optional.empty();
        this.effects = Optional.empty();
        this.type = ResponseType.EMPTY_REPLY;
    }

    public Value(
            R response,
            S state,
            Broadcast<?> broadcast,
            Forward forward,
            Pipe pipe,
            List<SideEffect> effects,
            ResponseType type) {
        this.response = response;
        this.state = state;
        this.broadcast = Optional.ofNullable(broadcast);
        this.forward =  Optional.ofNullable(forward);
        this.pipe =  Optional.ofNullable(pipe);
        this.effects =  Optional.ofNullable(effects);
        this.type = type;
    }

    public R getResponse() {
        return response;
    }

    public S getState() {
        return state;
    }

    public Optional<Broadcast<?>> getBroadcast() {
        return broadcast;
    }

    public Optional<Forward> getForward() {
        return forward;
    }

    public Optional<Pipe> getPipe() {
        return pipe;
    }

    public Optional<List<SideEffect>> getEffects() {
        return effects;
    }

    public ResponseType getType() {
        return type;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Value{");
        sb.append("state=").append(state);
        sb.append(", value=").append(response);
        sb.append(", broadcast=").append(broadcast);
        sb.append(", forward=").append(forward);
        sb.append(", pipe=").append(pipe);
        sb.append(", effects=").append(effects);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }

    enum ResponseType {
        REPLY, NO_REPLY, EMPTY_REPLY
    }

    public static final class ActorValue<S extends GeneratedMessageV3, R extends GeneratedMessageV3> {
        private final List<SideEffect> effects = new ArrayList<>();
        private S state;
        private R response;
        private Broadcast<?> broadcast;
        private Forward forward;
        private Pipe pipe;

        public ActorValue() {
        }

        public static <S, V> ActorValue at() {
            return new ActorValue();
        }

        public ActorValue response(R value) {
            this.response = value;
            return this;
        }

        public ActorValue state(S state) {
            this.state = state;
            return this;
        }

        public ActorValue flow(Broadcast broadcast) {
            this.broadcast = broadcast;
            return this;
        }

        public ActorValue flow(Forward forward) {
            this.forward = forward;
            return this;
        }

        public ActorValue flow(Pipe pipe) {
            this.pipe = pipe;
            return this;
        }

        public ActorValue flow(SideEffect effect) {
            this.effects.add(effect);
            return this;
        }

        public ActorValue flow(List<SideEffect> effects) {
            this.effects.addAll(effects);
            return this;
        }

        public Value reply() {
            return new Value(this.response, this.state, this.broadcast, this.forward, this.pipe, this.effects, ResponseType.REPLY);
        }

        public Value noReply() {
            return new Value(this.response, this.state, this.broadcast, this.forward, this.pipe, this.effects, ResponseType.NO_REPLY);
        }

        public Value empty() {
            return new Value();
        }
    }
}
