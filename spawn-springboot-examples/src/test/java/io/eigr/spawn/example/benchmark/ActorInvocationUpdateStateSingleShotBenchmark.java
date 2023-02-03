package io.eigr.spawn.example.benchmark;

import io.eigr.spawn.example.App;
import io.eigr.spawn.example.MyBusinessMessage;
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
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@RunWith(SpringRunner.class)
public class ActorInvocationUpdateStateSingleShotBenchmark extends AbstractBenchmark {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActorInvocationUpdateStateSingleShotBenchmark.class);

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
    public void invokeUpdateStateOnSingletonActor() {
        try {
            MyBusinessMessage input = MyBusinessMessage.newBuilder()
                    .setValue(1)
                    .build();

            actorSystem.invoke(ACTOR_NAME, "sum", input, MyBusinessMessage.class);
        } catch (Exception e) {
            LOGGER.error("Error on make request to Actor", e);
        }
    }

}
