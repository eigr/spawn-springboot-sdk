package io.eigr.spawn.springboot.starter;

import com.google.protobuf.Any;
import io.eigr.functions.protocol.Protocol;
import io.eigr.functions.protocol.actors.ActorOuterClass;
import io.eigr.spawn.springboot.starter.autoconfigure.EnableSpawn;
import io.eigr.spawn.springboot.starter.autoconfigure.SpawnAutoConfiguration;
import io.eigr.spawn.springboot.starter.util.SpawnContainer;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;

@Log4j2
@EnableSpawn
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8091",
                "management.server.port=8091"
        })
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = SpawnAutoConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SpawnTest {
    public static final String SPAWN_PROXY_ACTORS_ACTOR_INVOKE_URL = "http://localhost:%s/api/v1/system/test-system/actors/actor-test-01/invoke";
    public static final String SPAWN_MEDIA_TYPE = "application/octet-stream";
    public static final String SPAWN_PROXY_ACTOR_SYSTEM_URL = "http://localhost:%s/api/v1/system";
    private final OkHttpClient client = new OkHttpClient();

    private static final DockerImageName MYSQL_57_IMAGE = DockerImageName.parse("mysql:5.7.34");

    /**
     *
     */
    //@Test
    public void shouldAnswerWithTrue() throws IOException, InterruptedException {
        Testcontainers.exposeHostPorts(8091);
        Network network = Network.newNetwork();
        try (MySQLContainer<?> mysql = new MySQLContainer<>(MYSQL_57_IMAGE)
                .withUsername("admin")
                .withPassword("admin")
                //.withExposedPorts(3306)
                .withNetwork(network)
                .withNetworkAliases("mysql")
                .withDatabaseName("eigr-functions-db");
             SpawnContainer container = SpawnContainer.builder()
                     .network(network)
                     .stateStoreHost("mysql")
                     .build()) {

            mysql.start();
            Thread.sleep(20000);
            container.start();

            HashMap<String, ActorOuterClass.Actor> actors = new HashMap<>();
            for (int i = 0; i < 100; i++) {
                String actorName = String.format("actor-test-0%s", i);
                actors.put(actorName, makeActor(actorName, 1));
            }

            ActorOuterClass.Registry registry = ActorOuterClass.Registry.newBuilder()
                    .putAllActors(actors)
                    .build();

            ActorOuterClass.ActorSystem actorSystem = ActorOuterClass.ActorSystem.newBuilder()
                    .setName("test-system")
                    .setRegistry(registry)
                    .build();

            Protocol.ServiceInfo si = Protocol.ServiceInfo.newBuilder()
                    .setServiceName("jvm-sdk")
                    .setServiceVersion("0.1.1")
                    .setServiceRuntime(System.getProperty("java.version"))
                    .setProtocolMajorVersion(1)
                    .setProtocolMinorVersion(1)
                    .build();

            Protocol.RegistrationRequest registration = Protocol.RegistrationRequest.newBuilder()
                    .setServiceInfo(si)
                    .setActorSystem(actorSystem)
                    .build();

            RequestBody body = RequestBody.create(
                    registration.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

            Request request = new Request.Builder()
                    .url(String.format(SPAWN_PROXY_ACTOR_SYSTEM_URL, container.getExposedPort()))
                    .post(body)
                    .build();

            log.info("Send registration request...");

            Call call = client.newCall(request);
            Response callInvocationResponse;
            try (Response response = call.execute()) {

                assertThat(response.code(), equalTo(200));
                assert response.body() != null;
                Protocol.RegistrationResponse registrationResponse = Protocol.RegistrationResponse
                        .parseFrom(response.body().bytes());

                log.info("Registration response: {}", registrationResponse);

                // Send Invocation to Actor
                MyBusinessMessage valueMessage = MyBusinessMessage.newBuilder()
                        .setValue(10)
                        .build();

                Any stateValue = Any.pack(valueMessage);

                Protocol.InvocationRequest invocationRequest = Protocol.InvocationRequest.newBuilder()
                        .setAsync(false)
                        .setSystem(actorSystem)
                        .setActor(makeActor("actor-test-01", 1))
                        .setActionName("someFunction")
                        .setValue(stateValue)
                        .build();

                RequestBody invocationBody = RequestBody.create(
                        invocationRequest.toByteArray(), MediaType.parse(SPAWN_MEDIA_TYPE));

                Request httpInvocationRequest = new Request.Builder()
                        .url(String.format(SPAWN_PROXY_ACTORS_ACTOR_INVOKE_URL, container.getExposedPorts().get(0)))
                        .post(invocationBody)
                        .build();

                Thread.sleep(3000);

                for (int i = 0; i < 1; i++) {
                    try {
                        log.info("Send Invocation request...");
                        Call invocationCall = client.newCall(httpInvocationRequest);
                        callInvocationResponse = invocationCall.execute();

                        assertThat(callInvocationResponse.code(), equalTo(200));
                        assert callInvocationResponse.body() != null;
                        Protocol.InvocationResponse invocationResponse = Protocol.InvocationResponse
                                .parseFrom(callInvocationResponse.body().bytes());

                        log.info("Invocation response: {}", invocationResponse);
                        Any updatedState = invocationResponse.getValue();
                        MyBusinessMessage updatedMyBusinessMessage = updatedState.unpack(MyBusinessMessage.class);
                        log.info("MyBusinessMessage result: {}", updatedMyBusinessMessage.getValue());
                        assertThat(response.code(), equalTo(200));
                    } catch (Exception e) {
                        log.error("Error on call Actor", e);
                    }
                }

            }

            Thread.sleep(5000);
            assertTrue(true);
        }
    }

    private ActorOuterClass.Actor makeActor(String name, Integer state) {

        MyBusinessMessage valueMessage = MyBusinessMessage.newBuilder()
                .setValue(state)
                .build();

        Any stateValue = Any.pack(valueMessage);

        ActorOuterClass.ActorState initialState = ActorOuterClass.ActorState.newBuilder()
                .setState(stateValue)
                .build();

        ActorOuterClass.ActorSnapshotStrategy snapshotStrategy =
                ActorOuterClass.ActorSnapshotStrategy.newBuilder()
                        .setTimeout(
                                ActorOuterClass.TimeoutStrategy.newBuilder()
                                        .setTimeout(10000)
                                        .build()
                        )
                        .build();

        ActorOuterClass.ActorDeactivationStrategy deactivationStrategy =
                ActorOuterClass.ActorDeactivationStrategy.newBuilder()
                        .setTimeout(
                                ActorOuterClass.TimeoutStrategy.newBuilder()
                                        .setTimeout(60000)
                                        .build()
                        )
                        .build();

        ActorOuterClass.ActorSettings settings = ActorOuterClass.ActorSettings.newBuilder()
                .setStateful(true)
                .setSnapshotStrategy(snapshotStrategy)
                .setDeactivationStrategy(deactivationStrategy)
                .build();

        return ActorOuterClass.Actor.newBuilder()
                .setId(
                        ActorOuterClass.ActorId.newBuilder()
                                .setName(name)
                                .build()
                )
                .setSettings(settings)
                .setState(initialState)
                .build();
    }

}
