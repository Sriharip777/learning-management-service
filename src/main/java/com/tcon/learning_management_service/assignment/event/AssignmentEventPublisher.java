package com.tcon.learning_management_service.assignment.event;

import com.tcon.learning_management_service.assignment.entity.Submission;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssignmentEventPublisher {

    private final KafkaTemplate<String,Object> kafkaTemplate;


    public void publishAssignmentSubmitted(
            Submission submission)
    {
        kafkaTemplate.send(
                "assignment-submitted",
                submission);
    }


    public void publishAssignmentEvaluated(
            Submission submission)
    {
        kafkaTemplate.send(
                "assignment-evaluated",
                submission);
    }

}