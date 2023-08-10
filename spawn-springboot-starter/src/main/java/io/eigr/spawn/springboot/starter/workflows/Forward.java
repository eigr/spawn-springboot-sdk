package io.eigr.spawn.springboot.starter.workflows;

import io.eigr.functions.protocol.Protocol;

import java.util.Objects;

public final class Forward {

    private final String actor;

    private final String command;

    private Forward(String actor, String command) {
        this.actor = actor;
        this.command = command;
    }

    public Forward to(String actor, String command) {
        return new Forward(actor, command);
    }

    public String getActor() {
        return actor;
    }

    public Protocol.Forward build() {
        return Protocol.Forward.newBuilder()
                .setActor(this.actor)
                .setActionName(this.command)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Forward forward = (Forward) o;
        return Objects.equals(actor, forward.actor) && Objects.equals(command, forward.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(actor, command);
    }
}
