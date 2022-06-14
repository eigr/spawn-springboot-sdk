package io.eigr.spawn.springboot.starter.internal;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OkHttpSpawnClient implements SpawnClient {
    public static final String SPAWN_MEDIA_TYPE = "application/octet-stream";
    private static final Logger log = LoggerFactory.getLogger(OkHttpSpawnClient.class);
    private static final String SPAWN_REGISTER_URI = "/api/v1/system";

    private static final OkHttpClient client = new OkHttpClient();

    @Autowired
    private SpawnProperties properties;

    @Override
    public Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws Exception {
        log.debug("Send registration request");
        RequestBody body = RequestBody.create(
                registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request request = new Request.Builder()
                .url(makeURLFrom(SPAWN_REGISTER_URI))
                .post(body)
                .build();

        Response callInvocationResponse;
        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            assert response.body() != null;
            return Protocol.RegistrationResponse
                    .parseFrom(response.body().bytes());
        } catch (Exception e) {
            log.error("Error registering Actors", e);
            throw new Exception(e);
        }
    }

    @Override
    public Protocol.InvocationResponse invoke(Protocol.InvocationRequest request) throws Exception {
        return null;
    }

    private String makeURLFrom(String uri) {
        return String.format("http://%s:%S/%s",
                this.properties.getProxyInterface(), this.properties.getProxyPort(), uri);
    }
}
