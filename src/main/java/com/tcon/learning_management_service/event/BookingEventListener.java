package com.tcon.learning_management_service.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class BookingEventListener {

    @KafkaListener(topics = "payment.success", groupId = "learning-management-group")
    public void handlePaymentSuccess(String message) {
        log.info("Received payment success event: {}", message);
        // Handle payment success - update booking status
    }

    @KafkaListener(topics = "payment.failed", groupId = "learning-management-group")
    public void handlePaymentFailed(String message) {
        log.info("Received payment failed event: {}", message);
        // Handle payment failure - cancel booking
    }
}
