package io.eigr.spawn.example;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = {
                "server.port=8090",
                "management.server.port=8090"
        })
@RunWith(SpringJUnit4ClassRunner.class)
public class AppTest {

    @Autowired
    private ApplicationContext context;

    @Test
    public void shouldStartApplication() {

    }
}
