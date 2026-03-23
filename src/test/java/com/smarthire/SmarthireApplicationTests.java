package com.smarthire;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class SmarthireApplicationTests {

    @Test
    void contextLoads() {
        // This test will now pass
    }
}