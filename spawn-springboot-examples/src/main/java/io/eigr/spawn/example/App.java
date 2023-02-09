package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.ActionRequest;
import io.eigr.spawn.springboot.starter.ActionResponse;
import io.eigr.spawn.springboot.starter.SpawnSystem;
import io.eigr.spawn.springboot.starter.autoconfigure.EnableSpawn;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;

@Log4j2
@EnableSpawn
@SpringBootApplication
@EntityScan("io.eigr.spawn.example")
public class App {

    private static final int requestCount = 200;
    private static final int actors_iterations = 1;

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            SpawnSystem actorSystem = ctx.getBean(SpawnSystem.class);
            createActors(actorSystem, actors_iterations);

            //Thread.sleep(1000);

            //sequentialSumInvokes(actorSystem, actors_iterations, requestCount);

            //Thread.sleep(1000);

            //parallelSumAndGetInvokes(actorSystem, actors_iterations, requestCount);
        };
    }

    private void createActors(SpawnSystem actorSystem, int interations) throws Exception {
        for (int i = 0; i < actors_iterations; i++) {
            String actorName = String.format("concreteActor-%s", i);
            log.info("Let's spawning Actor {}", actorName);
            Instant instant = Instant.now();
            actorSystem.spawn(actorName, AbstractActor.class);
            log.info("Spawning Actor Time Elapsed: {}ms",
                    ChronoUnit.MILLIS.between(instant, Instant.now()));
        }
    }

    private void sequentialSumInvokes(SpawnSystem actorSystem, int actors_iterations, int requestCount) {
        IntStream callStream = IntStream.range(0, actors_iterations);

        callStream.forEach(actorIndex -> {
            String actorName = String.format("concreteActor-%s", actorIndex);

            IntStream.range(0, requestCount)
                    .forEach(index -> {
                        try {
                            log.info("Let's invoke {}", actorName);

                            ActionRequest request =
                                    ActionRequest.<MyBusinessMessage, MyBusinessMessage>of()
                                            .actorName(actorName)
                                            .action("sum")
                                            .value(MyBusinessMessage.newBuilder().setValue(1).build())
                                            .build();

                            actorSystem.invoke(request);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        });
    }

    private void parallelSumAndGetInvokes(SpawnSystem actorSystem, int actors_iterations, int requestCount) {
        IntStream callStream = IntStream.range(0, actors_iterations);

        Instant initialLoop = Instant.now();
        callStream.parallel().forEach(actorIndex -> {
            String actorName = String.format("concreteActor-%s", actorIndex);

            IntStream.range(0, requestCount)
                    .forEach(index -> {
                        try {
                            Instant noRequest = Instant.now();
                            ActionRequest request = ActionRequest.of(actorName, "sum")
                                    .value(
                                            MyBusinessMessage.newBuilder()
                                                    .setValue(1).build()
                                    )
                                    .responseType(MyBusinessMessage.class)
                                    .build();


                            ActionResponse response = actorSystem.invoke(request);
                            MyBusinessMessage result = (MyBusinessMessage) response.getValue().get();
                            log.info("Actor Invocation No Cached Request Time Elapsed: {}ms",
                                    ChronoUnit.MILLIS.between(noRequest, Instant.now()));

                            Instant cachedRequest = Instant.now();
                            ActionRequest getRequest = ActionRequest.of(actorName, "sum")
                                    .responseType(MyState.class)
                                    .build();

                            actorSystem.invoke(getRequest);
                            log.info("Actor Invocation Cached Request Time Elapsed: {}ms",
                                    ChronoUnit.MILLIS.between(cachedRequest, Instant.now()));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        });
        log.info("Actor Invocations {} interactions in Request Time Elapsed: {}s",
                (actors_iterations + requestCount), Duration.between(initialLoop, Instant.now()).getSeconds());
    }
}