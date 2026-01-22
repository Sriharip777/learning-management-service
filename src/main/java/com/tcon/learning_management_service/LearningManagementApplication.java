package com.tcon.learning_management_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
@EnableAsync
@EnableScheduling

public class LearningManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningManagementApplication.class, args);
        log.info("Learning Management Service started successfully");
    }
}

