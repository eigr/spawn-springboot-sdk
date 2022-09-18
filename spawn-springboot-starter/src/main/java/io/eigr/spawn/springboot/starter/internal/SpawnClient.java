package io.eigr.spawn.springboot.starter.internal;

import io.eigr.functions.protocol.Protocol;

import java.io.IOException;

public interface SpawnClient {

    Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws Exception;

    public Protocol.SpawnResponse spawn(Protocol.SpawnRequest registration) throws Exception;
    Protocol.InvocationResponse invoke(Protocol.InvocationRequest request) throws Exception;
}
