package com.tcon.learning_management_service.event;


import com.tcon.learning_management_service.entity.ClassSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class SessionEventPublisher {

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;

    @Value("${app.kafka.topics.session-started:session.started}")
    private String sessionStartedTopic;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Autowired
    public SessionEventPublisher(Optional<KafkaTemplate<String, Object>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishSessionScheduled(ClassSession session) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping session scheduled event: {}", session.getId());
            return;
        }

        log.info("Publishing session scheduled event: {}", session.getId());
        try {
            kafkaTemplate.get().send("session.scheduled", session.getId(), session);
        } catch (Exception e) {
            log.error("Failed to publish session scheduled event", e);
        }
    }

    public void publishSessionStarted(ClassSession session) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping session started event: {}", session.getId());
            return;
        }

        log.info("Publishing session started event: {}", session.getId());
        try {
            kafkaTemplate.get().send(sessionStartedTopic, session.getId(), session);
        } catch (Exception e) {
            log.error("Failed to publish session started event", e);
        }
    }

    public void publishSessionCompleted(ClassSession session) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping session completed event: {}", session.getId());
            return;
        }

        log.info("Publishing session completed event: {}", session.getId());
        try {
            kafkaTemplate.get().send("session.completed", session.getId(), session);
        } catch (Exception e) {
            log.error("Failed to publish session completed event", e);
        }
    }

    public void publishSessionReminder(ClassSession session) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping session reminder event: {}", session.getId());
            return;
        }

        log.info("Publishing session reminder event: {}", session.getId());
        try {
            kafkaTemplate.get().send("session.reminder", session.getId(), session);
        } catch (Exception e) {
            log.error("Failed to publish session reminder event", e);
        }
    }
}
