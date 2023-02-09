package io.eigr.spawn.example.benchmark;

import io.eigr.spawn.example.App;
import io.eigr.spawn.example.MyBusinessMessage;
import io.eigr.spawn.springboot.starter.ActionRequest;
import io.eigr.spawn.springboot.starter.SpawnSystem;
import org.junit.runner.RunWith;
import org.openjdk.jmh.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@RunWith(SpringRunner.class)
public class ActorInvocationGetStateBenchmark extends AbstractBenchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActorInvocationGetStateBenchmark.class);

    private static final String ACTOR_NAME = "joe";

    private ApplicationContext context;

    private SpawnSystem actorSystem;

    /*
     * There is no @Test annotated method within here, because the AbstractBenchmark
     * defines one, which spawns the JMH runner. This class only contains JMH/Benchmark
     * related code.
     */

    @Setup(Level.Trial)
    public void setupBenchmark() throws Exception {
        this.context = new SpringApplication(App.class).run();
        this.actorSystem = this.context.getBean(SpawnSystem.class);
        this.actorSystem.registerAllActors();
    }

    @Benchmark
    public void invokeGetStateOnSingletonActor() {
        try {
            ActionRequest request = ActionRequest.of(ACTOR_NAME, "get")
                    .responseType(MyBusinessMessage.class)
                    .build();

            actorSystem.invoke(request);
        } catch (Exception e) {
            LOGGER.error("Error on make request to Actor", e);
        }
    }
}
