package com.tcon.learning_management_service.assignment.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AssignmentEventListener {

    @KafkaListener(
            topics = "assignment-submitted",
            groupId = "learning-management-group")
    public void handleAssignmentSubmitted(Object event)
    {
        log.info("Assignment Submitted Event Received: {}", event);
    }


    @KafkaListener(
            topics = "assignment-evaluated",
            groupId = "learning-management-group")
    public void handleAssignmentEvaluated(Object event)
    {
        log.info("Assignment Evaluated Event Received: {}", event);
    }

}