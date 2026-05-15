package com.tcon.learning_management_service.event;

import com.tcon.learning_management_service.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
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
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BOOKING_CREATED");
            event.put("bookingId", booking.getId());
            event.put("classSessionId", booking.getSessionId());
            event.put("sessionId", booking.getSessionId());
            event.put("studentId", booking.getStudentId());
            event.put("teacherId", booking.getTeacherId());
            event.put("scheduledStartTime",
                    booking.getSessionStartTime() != null ? booking.getSessionStartTime().toString() : "");
            event.put("timestamp", Instant.now().toString());

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published BOOKING_CREATED for booking {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish BOOKING_CREATED for booking {}", booking != null ? booking.getId() : null, e);
        }
    }

    public void publishBookingConfirmed(Booking booking) {
        try {
            Instant startTime = booking.getSessionStartTime();
            Instant endTime = booking.getSessionEndTime();
            Integer duration = booking.getDurationMinutes();

            if ((startTime == null || duration == null || duration == 0)
                    && booking.getSessions() != null
                    && !booking.getSessions().isEmpty()) {

                Booking.SessionTime first = booking.getSessions().get(0);
                startTime = first.getStartTime();
                endTime = first.getEndTime();

                if (startTime != null && endTime != null) {
                    duration = (int) Duration.between(startTime, endTime).toMinutes();
                }

                log.info("Batch booking using first slot for booking {} start {} end {} duration {}",
                        booking.getId(), startTime, endTime, duration);
            }

            if (startTime == null) {
                log.error("Cannot publish BOOKING_CONFIRMED because startTime is null for booking {}", booking.getId());
                return;
            }

            if (duration == null || duration == 0) {
                log.error("Cannot publish BOOKING_CONFIRMED because duration is invalid for booking {}", booking.getId());
                return;
            }

            String classSessionId = booking.getSessionId();
            if (classSessionId == null || classSessionId.isBlank()) {
                log.error("Cannot publish BOOKING_CONFIRMED because classSessionId is null for booking {}", booking.getId());
                return;
            }

            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BOOKING_CONFIRMED");
            event.put("bookingId", booking.getId());
            event.put("classSessionId", classSessionId);
            event.put("sessionId", classSessionId);
            event.put("teacherId", booking.getTeacherId());
            event.put("studentId", booking.getStudentId());
            event.put("parentId", booking.getParentId() != null ? booking.getParentId() : "");
            event.put("scheduledStartTime", startTime.toString());
            event.put("scheduledEndTime", endTime != null ? endTime.toString() : "");
            event.put("durationMinutes", duration);
            event.put("subject", booking.getSubject() != null ? booking.getSubject() : "One-on-One Class");
            event.put("timestamp", Instant.now().toString());

            kafkaTemplate.send(TOPIC, booking.getId(), event);

            log.info("Published BOOKING_CONFIRMED for booking {}", booking.getId());
            log.info("classSessionId: {}", classSessionId);
            log.info("teacherId: {}", booking.getTeacherId());
            log.info("studentId: {}", booking.getStudentId());
            log.info("startTime: {}", startTime);
            log.info("durationMinutes: {}", duration);

        } catch (Exception e) {
            log.error("Failed to publish BOOKING_CONFIRMED for booking {}", booking != null ? booking.getId() : null, e);
        }
    }

    public void publishBookingApproved(Booking booking) {
        try {
            Instant startTime = booking.getSessionStartTime();
            Instant endTime = booking.getSessionEndTime();
            Integer duration = booking.getDurationMinutes();

            if ((startTime == null || duration == null || duration == 0)
                    && booking.getSessions() != null
                    && !booking.getSessions().isEmpty()) {

                Booking.SessionTime first = booking.getSessions().get(0);
                startTime = first.getStartTime();
                endTime = first.getEndTime();

                if (startTime != null && endTime != null) {
                    duration = (int) Duration.between(startTime, endTime).toMinutes();
                }
            }

            String classSessionId = booking.getSessionId();

            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "BOOKING_APPROVED");
            event.put("bookingId", booking.getId());
            event.put("classSessionId", classSessionId);
            event.put("sessionId", classSessionId);
            event.put("teacherId", booking.getTeacherId());
            event.put("studentId", booking.getStudentId());
            event.put("parentId", booking.getParentId() != null ? booking.getParentId() : "");
            event.put("scheduledStartTime", startTime != null ? startTime.toString() : "");
            event.put("scheduledEndTime", endTime != null ? endTime.toString() : "");
            event.put("durationMinutes", duration != null ? duration : 60);
            event.put("subject", booking.getSubject() != null ? booking.getSubject() : "One-on-One Class");
            event.put("timestamp", Instant.now().toString());

            kafkaTemplate.send(TOPIC, booking.getId(), event);

            log.info("Published BOOKING_APPROVED for booking {}", booking.getId());
            log.info("classSessionId: {}", classSessionId);
            log.info("teacherId: {}", booking.getTeacherId());
            log.info("studentId: {}", booking.getStudentId());
            log.info("startTime: {}", startTime);
            log.info("durationMinutes: {}", duration);

        } catch (Exception e) {
            log.error("Failed to publish BOOKING_APPROVED for booking {}", booking != null ? booking.getId() : null, e);
        }
    }

    public void publishBookingRejected(Booking booking) {
        try {
            BookingEvent event = BookingEvent.builder()
                    .eventType("BOOKING_REJECTED")
                    .bookingId(booking.getId())
                    .sessionId(booking.getSessionId())
                    .studentId(booking.getStudentId())
                    .teacherId(booking.getTeacherId())
                    .cancellationReason(booking.getCancellationReason())
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published BOOKING_REJECTED for booking {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish BOOKING_REJECTED for booking {}", booking != null ? booking.getId() : null, e);
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
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("Published BOOKING_CANCELLED for booking {}", booking.getId());
        } catch (Exception e) {
            log.error("Failed to publish BOOKING_CANCELLED for booking {}", booking != null ? booking.getId() : null, e);
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
        private Instant sessionStartTime;
        private String cancellationReason;
        private BigDecimal refundAmount;
        private Instant timestamp;
    }
}