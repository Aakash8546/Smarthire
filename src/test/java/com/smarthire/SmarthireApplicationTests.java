package com.smarthire;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")  // This is CRITICAL - forces test profile
class SmarthireApplicationTests {

    @Test
    void contextLoads() {
        System.out.println("✅ Application context loaded successfully with H2 database!");
    }
}