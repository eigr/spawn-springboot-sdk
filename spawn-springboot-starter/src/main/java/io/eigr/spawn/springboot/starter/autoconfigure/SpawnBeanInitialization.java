package io.eigr.spawn.springboot.starter.autoconfigure;

import io.eigr.spawn.springboot.internal.GlobalEnvironment;
import io.eigr.spawn.springboot.internal.SpawnActorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
@Component
public class SpawnBeanInitialization {
    private final Logger log = LoggerFactory.getLogger(SpawnBeanInitialization.class);
    private final  Environment env;
    private final SpawnActorController actorController;

    @Autowired
    public SpawnBeanInitialization(SpawnActorController actorController, Environment env) {
        this.env = env;
        this.actorController = actorController;
        GlobalEnvironment.setEnvironment(env);
    }
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws Exception {
        if(env.acceptsProfiles("!test")) {
            log.debug("Registering Actors...");
            actorController.register();
        }
    }
}
