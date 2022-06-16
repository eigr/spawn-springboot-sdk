package io.eigr.spawn.springboot.starter.internal;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.net.ConnectException;

@Service
public class OkHttpSpawnClient implements SpawnClient {
    public static final String SPAWN_MEDIA_TYPE = "application/octet-stream";
    private static final String SPAWN_REGISTER_URI = "/api/v1/system";
    private static final Logger log = LoggerFactory.getLogger(OkHttpSpawnClient.class);

    private static final OkHttpClient client = new OkHttpClient();

    @Autowired
    private SpawnProperties properties;

    @Override
    @Retryable(value = ConnectException.class,
            maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws Exception {
        log.debug("Send registration request");
        RequestBody body = RequestBody.create(registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request request = new Request.Builder().url(makeURLFrom(SPAWN_REGISTER_URI)).post(body).build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            assert response.body() != null;
            return Protocol.RegistrationResponse.parseFrom(response.body().bytes());
        } catch (Exception e) {
            log.error("Error registering Actors", e);
            throw new Exception(e);
        }
    }

    @Override
    @Retryable(value = ConnectException.class,
            maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public Protocol.InvocationResponse invoke(Protocol.InvocationRequest request) throws Exception {
        RequestBody body = RequestBody.create(
                request.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request invocationRequest = new Request.Builder()
                .url(makeURLForSystemAndActor(request.getSystem().getName(), request.getActor().getName()))
                .post(body)
                .build();

        Call invocationCall = client.newCall(invocationRequest);
        Response callInvocationResponse = invocationCall.execute();

        return Protocol.InvocationResponse
                .parseFrom(callInvocationResponse.body().bytes());
    }

    private String makeURLForSystemAndActor(String systemName, String actorName) {
        String uri = String.format("/api/v1/system/%s/actors/%s/invoke", systemName, actorName);
        return makeURLFrom(uri);
    }

    private String makeURLFrom(String uri) {
        return String.format("http://%s:%S%s", this.properties.getProxyInterface(), this.properties.getProxyPort(), uri);
    }
}
