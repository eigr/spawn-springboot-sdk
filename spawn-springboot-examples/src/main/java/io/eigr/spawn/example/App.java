package io.eigr.spawn.example;

import io.eigr.spawn.springboot.starter.autoconfigure.EnableSpawn;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@EnableSpawn
@EntityScan("io.eigr.spawn.example")
@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
