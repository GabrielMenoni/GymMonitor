package com.example.accesscontrol;

import com.example.accesscontrol.config.RabbitMQTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(RabbitMQTestConfig.class)
class AccessControlApplicationTests {

    @Test
    void contextLoads() {
    }
}
