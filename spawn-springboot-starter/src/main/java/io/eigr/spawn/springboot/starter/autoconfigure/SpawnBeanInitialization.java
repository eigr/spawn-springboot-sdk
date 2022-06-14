package io.eigr.spawn.springboot.starter.autoconfigure;

import io.eigr.spawn.springboot.starter.internal.SpawnActorController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
@Component
public class SpawnBeanInitialization {
    private final Logger log = LoggerFactory.getLogger(SpawnBeanInitialization.class);
    private final SpawnActorController actorController;
    @Autowired
    public SpawnBeanInitialization(SpawnActorController actorController) {
        this.actorController = actorController;
    }
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws Exception {
        log.debug("Registering Actors...");
        actorController.register();
    }
}
