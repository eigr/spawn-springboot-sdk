package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.SpawnSystem;
import io.eigr.spawn.springboot.starter.autoconfigure.EnableSpawn;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@Log4j2
@EnableSpawn
@SpringBootApplication
@EntityScan("io.eigr.spawn.example")
public class App {
    public static void main(String[] args) {SpringApplication.run(App.class, args);}

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {
            SpawnSystem actorSystem = ctx.getBean(SpawnSystem.class);
            log.info("Let's invoke some Actor");
            for (int i = 0; i < 1000; i++) {
                Example.MyBusinessMessage arg = Example.MyBusinessMessage.newBuilder()
                        .setValue(i)
                        .build();

                Example.MyBusinessMessage result = (Example.MyBusinessMessage) actorSystem.invoke("joe", "sum", arg, Example.MyBusinessMessage.class);
                log.info("Actor invoke sum result: {}", result.getValue());
            }
        };
    }
}