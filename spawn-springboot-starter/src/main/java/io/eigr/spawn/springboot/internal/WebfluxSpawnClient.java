package io.eigr.spawn.springboot.internal;

import io.eigr.functions.protocol.Protocol;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Log4j2
//@Service
public class WebfluxSpawnClient implements SpawnClient {
    public static final String SPAWN_MEDIA_TYPE = "application/octet-stream";
    private static final String SPAWN_REGISTER_URI = "/api/v1/system";

    private static final String SPAWN_ACTOR_SPAWN = "/api/v1/system/%s/actors/spawn";

    @Value("${io.eigr.spawn.springboot.starter.proxyUdsEnable:false}")
    private boolean isUdsEnable;

    @Value("${io.eigr.spawn.springboot.starter.proxyUdsSocketFilePath}")
    private String udsSocketFilePath;

    private WebClient client;

    @Autowired
    private SpawnProperties properties;

    private File socketFile;

    @PostConstruct
    public void setup() throws IOException {
        if (isUdsEnable) {
            throw new IllegalArgumentException("Unix Domain Socket not supported with WebfluxSpawnClient");
        } else {
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
                    .responseTimeout(Duration.ofMillis(60000))
                    .doOnConnected(conn ->
                            conn.addHandlerLast(new ReadTimeoutHandler(60000, TimeUnit.MILLISECONDS))
                                    .addHandlerLast(new WriteTimeoutHandler(60000, TimeUnit.MILLISECONDS)));

            this.client = WebClient.builder()
                    .baseUrl(getRootUrl())
                    .defaultHeader(HttpHeaders.CONNECTION, "Keep-Alive")
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build();
        }
    }

    @Override
    public Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws Exception {
        return null;
    }

    @Override
    public Protocol.SpawnResponse spawn(Protocol.SpawnRequest registration) throws Exception {
        return null;
    }

    @Override
    public Protocol.InvocationResponse invoke(Protocol.InvocationRequest request) throws Exception {
        return null;
    }

    /*
    @Override
    @Retryable(value = ConnectException.class,
            maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(delayExpression = "${retry.maxDelay}"))
    public Protocol.RegistrationResponse register(Protocol.RegistrationRequest registration) throws Exception {
        log.debug("Send registration request");
        RequestBody body = RequestBody.create(registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

        Request request = new Request.Builder().url(makeURLFrom(SPAWN_REGISTER_URI)).post(body).build();

        Mono<byte[]> response = client.post()
                .uri(uriBuilder ->
                        uriBuilder.pathSegment(SPAWN_REGISTER_URI).build())
                .bodyValue(registration.toByteArray())
                .retrieve()
                .bodyToMono(byte[].class);

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            assert response.body() != null;
            return Protocol.RegistrationResponse.parseFrom(
                    Objects.requireNonNull(response.body()
                    ).bytes());
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
                .url(makeSpawnURLFrom(registration.getActors(0).getSystem()))
                .post(body).build();

        Call call = client.newCall(request);
        try (Response response = call.execute()) {
            assert response.body() != null;
            return Protocol.SpawnResponse.parseFrom(
                    Objects.requireNonNull(response.body()
                    ).bytes());
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
                .url(makeURLForSystemAndActor(request.getSystem().getName(), request.getActor().getId().getName()))
                .post(body)
                .build();

        Call invocationCall = client.newCall(invocationRequest);
        Response callInvocationResponse = invocationCall.execute();

        return Protocol.InvocationResponse
                .parseFrom(Objects.requireNonNull(callInvocationResponse.body()).bytes());
    }
    */

    private String getRootUrl() {
        if (isUdsEnable) {
            return String.format("http://%s", this.properties.getProxyInterface());
        }
        return String.format("http://%s:%S%s", this.properties.getProxyInterface(), this.properties.getProxyPort());
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
