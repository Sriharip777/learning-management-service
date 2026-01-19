package com.tcon.learning_management_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableMongoRepositories(basePackages = "com.tcon.learning_management_service.repository")
@ComponentScan(basePackages = "com.tcon.learning_management_service")
public class LearningManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(LearningManagementApplication.class, args);
    }
}
