package com.tcon.learning_management_service.event;


import com.tcon.learning_management_service.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "booking-events";

    public void publishBookingCreated(Booking booking) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType("BOOKING_CREATED")
                    .bookingId(booking.getId())
                    .sessionId(booking.getSessionId())
                    .studentId(booking.getStudentId())
                    .teacherId(booking.getTeacherId())
                    .sessionStartTime(booking.getSessionStartTime())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published booking created event: {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking created event", e);
        }
    }

    public void publishBookingConfirmed(Booking booking) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType("BOOKING_CONFIRMED")
                    .bookingId(booking.getId())
                    .sessionId(booking.getSessionId())
                    .studentId(booking.getStudentId())
                    .teacherId(booking.getTeacherId())
                    .sessionStartTime(booking.getSessionStartTime())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published booking confirmed event: {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking confirmed event", e);
        }
    }

    public void publishBookingCancelled(Booking booking) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType("BOOKING_CANCELLED")
                    .bookingId(booking.getId())
                    .sessionId(booking.getSessionId())
                    .studentId(booking.getStudentId())
                    .teacherId(booking.getTeacherId())
                    .cancellationReason(booking.getCancellationReason())
                    .refundAmount(booking.getRefundAmount())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published booking cancelled event: {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking cancelled event", e);
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class BookingEvent {
        private String eventType;
        private String bookingId;
        private String sessionId;
        private String studentId;
        private String teacherId;
        private LocalDateTime sessionStartTime;
        private String cancellationReason;
        private java.math.BigDecimal refundAmount;
        private LocalDateTime timestamp;
    }
}
