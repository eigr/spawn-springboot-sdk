package io.eigr.spawn.springboot.starter.exceptions;

public class ActorInvokeException extends IllegalStateException {

    public ActorInvokeException() {}
    public ActorInvokeException(String message) {
        super(message);
    }
}
