package io.eigr.spawn.springboot.internal.exceptions;

public final class ActorInvokeException extends IllegalStateException {

    public ActorInvokeException() {}
    public ActorInvokeException(String message) {
        super(message);
    }
}
