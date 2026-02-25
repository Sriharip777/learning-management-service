package com.tcon.learning_management_service.assignment.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(
        basePackages = {
                "com.tcon.learning_management_service.assignment.repository"
        }
)
public class AssignmentMongoConfig {
}