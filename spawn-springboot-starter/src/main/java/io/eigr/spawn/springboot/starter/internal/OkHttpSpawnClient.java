package io.eigr.spawn.springboot.starter.internal;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import io.eigr.spawn.springboot.starter.internal.transport.uds.UnixDomainSocketFactory;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Set;

@Service
public class OkHttpSpawnClient implements SpawnClient {
    public static final String SPAWN_MEDIA_TYPE = "application/octet-stream";
    private static final Logger log = LoggerFactory.getLogger(OkHttpSpawnClient.class);
    private static final String SPAWN_REGISTER_URI = "/api/v1/system";

    private static final String SPAWN_ACTOR_SPAWN = "/api/v1/system/%s/actors/spawn";

    @Value("${io.eigr.spawn.springboot.starter.proxyUdsEnable}")
    private boolean isUdsEnable;

    @Value("${io.eigr.spawn.springboot.starter.proxyUdsSocketFilePath}")
    private String udsSocketFilePath;

    private OkHttpClient client;

    @Autowired
    private SpawnProperties properties;

    private File socketFile;

    @PostConstruct
    public void setup() throws IOException {
        if (isUdsEnable) {
            FileAttribute<Set<PosixFilePermission>> rwx =
                    PosixFilePermissions.asFileAttribute(
                            PosixFilePermissions.fromString("rwxrwxrwx"));

            Path path = Paths.get(udsSocketFilePath);
            if (!path.toFile().exists()) {
                socketFile = Files.createFile(path, rwx).toFile();

            } else {
                socketFile = path.toFile();
            }

            this.client = new OkHttpClient.Builder()
                    .socketFactory(new UnixDomainSocketFactory(socketFile))
                    .protocols(Arrays.asList(okhttp3.Protocol.HTTP_1_1))
                    .build();
        } else {
            this.client = new OkHttpClient();
        }
    }

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
    public Protocol.SpawnResponse spawn(Protocol.SpawnRequest registration) throws Exception {
        log.debug("Send registration request");
        RequestBody body = RequestBody.create(registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request request = new Request.Builder()
                .url(makeSpawnURLFrom(registration.getActorSystem().getName()))
                .post(body).build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            assert response.body() != null;
            return Protocol.SpawnResponse.parseFrom(response.body().bytes());
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
        if (isUdsEnable) {
            return String.format("http://%s%s", this.properties.getProxyInterface(), uri);
        }
        return String.format("http://%s:%S%s", this.properties.getProxyInterface(), this.properties.getProxyPort(), uri);
    }

    private String makeSpawnURLFrom(String systemName) {
        String uri = String.format(SPAWN_ACTOR_SPAWN, systemName);
        return makeURLFrom(uri);
    }
}
