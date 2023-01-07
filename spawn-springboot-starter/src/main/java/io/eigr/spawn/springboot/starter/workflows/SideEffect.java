package io.eigr.spawn.springboot.starter.workflows;

import io.eigr.functions.protocol.Protocol;

public final class SideEffect {

    public Protocol.SideEffect build() {
        return Protocol.SideEffect.newBuilder().build();
    }
}
