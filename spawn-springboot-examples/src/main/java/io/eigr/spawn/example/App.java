package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.SpawnSystem;
import io.eigr.spawn.springboot.starter.autoconfigure.EnableSpawn;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.stream.IntStream;

@Log4j2
@EnableSpawn
@SpringBootApplication
@EntityScan("io.eigr.spawn.example")
public class App {

    private final OkHttpClient client = new OkHttpClient();

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            SpawnSystem actorSystem = ctx.getBean(SpawnSystem.class);

            for (int i = 0; i < 200; i++) {
                String actorName = String.format("concreteActor-%s", i);
                log.info("Let's spawning Actor {}", actorName);
                actorSystem.spawn(actorName, AbstractActor.class);
            }

            Thread.sleep(20000);

            for (int i = 0; i < 200; i++) {
                String actorName = String.format("concreteActor-%s", i);

                IntStream.range(0, 1000)
                        .forEach(index -> {
                            try {
                                log.info("Let's invoke {}", actorName);
                                MyBusinessMessage input = MyBusinessMessage.newBuilder().setValue(1).build();

                                actorSystem.invoke(actorName, "sum", input, MyBusinessMessage.class);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

            }

            /*
            log.info("Let's invoke some Actor");
            for (int i = 0; i < 50000; i++) {
                MyBusinessMessage arg = MyBusinessMessage.newBuilder().setValue(i).build();

                Instant initialInvokeRequestTime = Instant.now();
                MyBusinessMessage sumResult =
                        (MyBusinessMessage) actorSystem.invoke("joe", "sum", arg, MyBusinessMessage.class);

                log.info("Actor invoke Sum Actor Action value result: {}. Request Time Elapsed: {}ms",
                        sumResult.getValue(), Duration.between(initialInvokeRequestTime, Instant.now()).toMillis());
            }

            Instant initialInvokeRequestTime = Instant.now();
            MyBusinessMessage getResult =
                    (MyBusinessMessage) actorSystem.invoke("joe", "get", MyBusinessMessage.class);
            log.info("Actor invoke Get Actor Action value result: {}. Request Time Elapsed: {}ms",
                    getResult.getValue(), Duration.between(initialInvokeRequestTime, Instant.now()).toMillis());
        };
            */



            /*
            HashMap<String, ActorOuterClass.Actor> actors = new HashMap<>();
            for (int i = 0; i < 60000; i++) {
                String actorName = String.format("actor-test-%s", i);
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
                    .url(SPAWN_PROXY_ACTORSYSTEM_URL)
                    .post(body)
                    .build();

            log.info("Send registration request...");
            Call call = client.newCall(request);
            try (Response response = call.execute()) {
                Protocol.RegistrationResponse registrationResponse = Protocol.RegistrationResponse
                        .parseFrom(response.body().bytes());
                log.info("Registration response: {}", registrationResponse);
            }

            Thread.sleep(5000);
            */

        };

    }

}