package com.example.apitest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TestPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestPlatformApplication.class, args);
    }
}
