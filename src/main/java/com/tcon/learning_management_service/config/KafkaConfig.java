package com.tcon.learning_management_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {

    @Value("${app.kafka.topics.booking-created:booking.created}")
    private String bookingCreatedTopic;

    @Value("${app.kafka.topics.session-started:session.started}")
    private String sessionStartedTopic;

    @Bean
    public NewTopic bookingCreatedTopic() {
        return TopicBuilder.name(bookingCreatedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sessionStartedTopic() {
        return TopicBuilder.name(sessionStartedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic sessionCompletedTopic() {
        return TopicBuilder.name("session.completed")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
