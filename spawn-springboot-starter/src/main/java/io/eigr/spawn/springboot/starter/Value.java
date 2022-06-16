package io.eigr.spawn.springboot.starter;

import com.google.protobuf.GeneratedMessageV3;

public final class Value<S extends GeneratedMessageV3, V extends GeneratedMessageV3> {

    enum ResponseType {
        REPLY, NO_REPLY, EMPTY_REPLY
    }

    private final S state;

    private final V value;

    private final ResponseType type;

    public Value() {
        this.state = null;
        this.value = null;
        this.type = ResponseType.EMPTY_REPLY;
    }

    public Value(V value, S state, ResponseType type) {
        this.value = value;
        this.state = state;
        this.type = type;
    }

    public V getValue() {
        return value;
    }

    public S getState() {
        return state;
    }

    public ResponseType getType() {
        return type;
    }

    public static final class ActorValue<S extends GeneratedMessageV3, V extends GeneratedMessageV3> {
        private S state;
        private V value;

        public ActorValue(){}

        public static <S, V> ActorValue at() {
            return new ActorValue();
        }

        public ActorValue value(V value) {
            this.value = value;
            return this;
        }

        public ActorValue state(S state){
            this.state = state;
            return this;
        }

        public Value reply() {
            return new Value(this.value, this.state, ResponseType.REPLY);
        }

        public Value noReply() {
            return new Value(this.value, this.state, ResponseType.NO_REPLY);
        }

        public Value empty() {
            return new Value();
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Value{");
        sb.append("state=").append(state);
        sb.append(", value=").append(value);
        sb.append(", type=").append(type);
        sb.append('}');
        return sb.toString();
    }
}
