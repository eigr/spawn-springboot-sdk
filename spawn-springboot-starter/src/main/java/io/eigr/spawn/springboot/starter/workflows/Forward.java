package io.eigr.spawn.springboot.starter.workflows;

import io.eigr.functions.protocol.Protocol;

public final class Forward {

    public Protocol.Forward build() {
        return Protocol.Forward.newBuilder().build();
    }
}
