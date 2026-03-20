package com.tcon.learning_management_service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableMongoAuditing
@EnableAsync
@EnableScheduling
@EnableFeignClients
public class LearningManagementApplication {

    private static final Logger log =
            LoggerFactory.getLogger(LearningManagementApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(LearningManagementApplication.class, args);
        log.info("Learning Management Service started successfully");
    }
}