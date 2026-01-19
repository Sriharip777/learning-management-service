package com.tcon.learning_management_service.event;


import com.tcon.learning_management_service.entity.Course;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class CourseEventPublisher {

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Autowired
    public CourseEventPublisher(Optional<KafkaTemplate<String, Object>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishCourseCreated(Course course) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping course created event: {}", course.getId());
            return;
        }

        log.info("Publishing course created event: {}", course.getId());
        try {
            kafkaTemplate.get().send("course.created", course.getId(), course);
        } catch (Exception e) {
            log.error("Failed to publish course created event", e);
        }
    }
}
