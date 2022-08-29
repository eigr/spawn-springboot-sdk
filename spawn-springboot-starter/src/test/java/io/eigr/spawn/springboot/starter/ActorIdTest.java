package io.eigr.spawn.springboot.starter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.net.URI;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
public class ActorIdTest {

    @Test
    public void shouldActorIdAValidUri() {
        String scheme = "actor";
        String instanceId = UUID.randomUUID().toString();
        String path = String.format("/test/actors/mike/%s", instanceId);
        String actorId = String.format("%s://system/test/actors/mike/%s", scheme, instanceId);
        URI joeActorId = URI.create(actorId);

        System.out.println(String.format("URI Scheme: %s",  joeActorId.getScheme()));
        System.out.println(String.format("URI Host: %s",  joeActorId.getHost()));
        System.out.println(String.format("URI Path: %s",  joeActorId.getPath()));
        System.out.println(String.format("URI Absolute: %s",  joeActorId.isAbsolute()));

        assertTrue(joeActorId.isAbsolute());
        assertEquals(scheme, joeActorId.getScheme());
        assertEquals("system", joeActorId.getHost());
        assertEquals(path, joeActorId.getPath());
        assertEquals("actor", joeActorId.getScheme());
    }
}
