package io.eigr.spawn.springboot.starter.workflows;

import io.eigr.functions.protocol.Protocol;

public final class Pipe {

    public Protocol.Pipe build() {
        return Protocol.Pipe.newBuilder().build();
    }
}
