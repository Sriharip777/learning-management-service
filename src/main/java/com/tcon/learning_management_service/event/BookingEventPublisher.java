package com.tcon.learning_management_service.event;


import com.tcon.learning_management_service.entity.Booking;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
public class BookingEventPublisher {

    private final Optional<KafkaTemplate<String, Object>> kafkaTemplate;

    @Value("${app.kafka.topics.booking-created:booking.created}")
    private String bookingCreatedTopic;

    @Value("${app.kafka.enabled:false}")
    private boolean kafkaEnabled;

    @Autowired
    public BookingEventPublisher(Optional<KafkaTemplate<String, Object>> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishBookingCreated(Booking booking) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping booking created event: {}", booking.getId());
            return;
        }

        log.info("Publishing booking created event: {}", booking.getId());
        try {
            kafkaTemplate.get().send(bookingCreatedTopic, booking.getId(), booking);
        } catch (Exception e) {
            log.error("Failed to publish booking created event", e);
        }
    }

    public void publishBookingConfirmed(Booking booking) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping booking confirmed event: {}", booking.getId());
            return;
        }

        log.info("Publishing booking confirmed event: {}", booking.getId());
        try {
            kafkaTemplate.get().send("booking.confirmed", booking.getId(), booking);
        } catch (Exception e) {
            log.error("Failed to publish booking confirmed event", e);
        }
    }

    public void publishBookingCancelled(Booking booking) {
        if (!kafkaEnabled || kafkaTemplate.isEmpty()) {
            log.debug("Kafka disabled - Skipping booking cancelled event: {}", booking.getId());
            return;
        }

        log.info("Publishing booking cancelled event: {}", booking.getId());
        try {
            kafkaTemplate.get().send("booking.cancelled", booking.getId(), booking);
        } catch (Exception e) {
            log.error("Failed to publish booking cancelled event", e);
        }
    }
}
