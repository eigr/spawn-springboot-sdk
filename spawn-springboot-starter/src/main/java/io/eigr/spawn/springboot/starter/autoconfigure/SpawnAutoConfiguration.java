package io.eigr.spawn.springboot.starter.autoconfigure;

import io.eigr.spawn.springboot.starter.SpawnSystem;
import io.eigr.spawn.springboot.internal.ActorClassGraphEntityScan;
import io.eigr.spawn.springboot.internal.SpawnActorController;
import io.eigr.spawn.springboot.internal.SpawnClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;

@EnableRetry
@Configuration
@PropertySource("classpath:retry.properties")
@EnableConfigurationProperties(SpawnProperties.class)
@ComponentScan(basePackages = "io.eigr.spawn.springboot")
public class SpawnAutoConfiguration {
    @Autowired
    private ApplicationContext context;

    @Autowired
    private SpawnClient client;

    @Autowired
    private SpawnProperties properties;

    @Bean
    @ConditionalOnMissingBean
    public ActorClassGraphEntityScan actorClassGraphEntityScan() {
        return new ActorClassGraphEntityScan(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpawnActorController actorController(SpawnProperties properties, ActorClassGraphEntityScan actorClassGraphEntityScan) {
        return new SpawnActorController(context, client, properties, actorClassGraphEntityScan);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpawnSystem actorSystem(SpawnActorController actorController) {
        return new SpawnSystem(actorController);
    }

}
