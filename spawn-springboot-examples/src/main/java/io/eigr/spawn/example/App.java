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

import java.time.Duration;
import java.time.Instant;

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
            log.info("Let's invoke some Actor");
            for (int i = 0; i < 24000; i++) {
                MyBusinessMessage arg = MyBusinessMessage.newBuilder().setValue(i).build();

                Instant initialInvokeRequestTime = Instant.now();
                MyBusinessMessage sumResult =
                        (MyBusinessMessage) actorSystem.invoke("zezinho", "sum", arg, MyBusinessMessage.class);

                log.info("Actor invoke Sum Actor Action value result: {}. Request Time Elapsed: {}ms",
                        sumResult.getValue(), Duration.between(initialInvokeRequestTime, Instant.now()).toMillis());
            }

            Instant initialInvokeRequestTime = Instant.now();
            MyBusinessMessage getResult =
                    (MyBusinessMessage) actorSystem.invoke("zezinho", "get", MyBusinessMessage.class);
            log.info("Actor invoke Get Actor Action value result: {}. Request Time Elapsed: {}ms",
                    getResult.getValue(), Duration.between(initialInvokeRequestTime, Instant.now()).toMillis());
        };
    }

}