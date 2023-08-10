package io.eigr.spawn.springboot.starter.workflows;

import io.eigr.functions.protocol.Protocol;

import java.util.Objects;

public final class Pipe {

    private final String actor;

    private final String command;

    private Pipe(String actor, String command) {
        this.actor = actor;
        this.command = command;
    }

    public Pipe to(String actor, String command) {
        return new Pipe(actor, command);
    }

    public String getActor() {
        return actor;
    }

    public String getCommand() {
        return command;
    }

    public Protocol.Pipe build() {
        return Protocol.Pipe.newBuilder()
                .setActor(this.actor)
                .setActionName(this.command)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pipe pipe = (Pipe) o;
        return Objects.equals(actor, pipe.actor) && Objects.equals(command, pipe.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actor, command);
    }
}
