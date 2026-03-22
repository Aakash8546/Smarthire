package com.smarthire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableAsync
public class SmarthireApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmarthireApplication.class, args);
        System.out.println("🚀 SmartHire Application Started Successfully!");
    }
}