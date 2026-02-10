package com.tcon.learning_management_service.event;

import com.tcon.learning_management_service.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
            Map<String, Object> event = new HashMap();
            event.put("eventType", "BOOKING_CONFIRMED");
            event.put("bookingId", booking.getId());
            event.put("classSessionId", booking.getSessionId());
            event.put("teacherId", booking.getTeacherId());
            event.put("studentId", booking.getStudentId());
            event.put("parentId", booking.getParentId());
            event.put("scheduledStartTime", booking.getSessionStartTime().toString());
            event.put("scheduledEndTime", booking.getSessionEndTime().toString()); // ✅ ADD THIS
            // ✅ Calculate duration if not set
            Integer duration = booking.getDurationMinutes();
            if (duration == null && booking.getSessionStartTime() != null && booking.getSessionEndTime() != null) {
                duration = (int) java.time.temporal.ChronoUnit.MINUTES.between(
                        booking.getSessionStartTime(),
                        booking.getSessionEndTime()
                );
                log.warn("⚠️ Duration was null for booking {}, calculated: {} minutes", booking.getId(), duration);
            }
            event.put("durationMinutes", duration);
            event.put("subject", booking.getSubject() != null ? booking.getSubject() : "One-on-One Class");
            event.put("timestamp", java.time.Instant.now().toString());

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("✅ Published BOOKING_CONFIRMED event for booking: {}", booking.getId());
            log.debug("Event data: {}", event);

        } catch (Exception e) {
            log.error("Failed to publish booking confirmed event", e);
        }
    }


    // ✅ ADD THIS METHOD
    public void publishBookingApproved(Booking booking) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType("BOOKING_APPROVED")
                    .bookingId(booking.getId())
                    .sessionId(booking.getSessionId())
                    .studentId(booking.getStudentId())
                    .teacherId(booking.getTeacherId())
                    .sessionStartTime(booking.getSessionStartTime())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published booking approved event: {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking approved event", e);
        }
    }

    // ✅ ADD THIS METHOD
    public void publishBookingRejected(Booking booking) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType("BOOKING_REJECTED")
                    .bookingId(booking.getId())
                    .sessionId(booking.getSessionId())
                    .studentId(booking.getStudentId())
                    .teacherId(booking.getTeacherId())
                    .cancellationReason(booking.getCancellationReason())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published booking rejected event: {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish booking rejected event", e);
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
