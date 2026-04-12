package com.tcon.learning_management_service.event;

import com.tcon.learning_management_service.booking.entity.Booking;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "booking-events";

    // ─── BOOKING CREATED ──────────────────────────────────────────────────────

    public void publishBookingCreated(Booking booking) {
        try {
            // ✅ FIX: Use Map to include classSessionId
            Map<String, Object> event = new HashMap<>();
            event.put("eventType",      "BOOKING_CREATED");
            event.put("bookingId",      booking.getId());
            event.put("classSessionId", booking.getSessionId()); // ✅
            event.put("sessionId",      booking.getSessionId()); // ✅
            event.put("studentId",      booking.getStudentId());
            event.put("teacherId",      booking.getTeacherId());
            event.put("scheduledStartTime", booking.getSessionStartTime() != null
                    ? booking.getSessionStartTime().toString() : "");
            event.put("timestamp",      java.time.Instant.now().toString());

            kafkaTemplate.send(TOPIC, booking.getId(), event);
            log.info("📤 Published BOOKING_CREATED for booking: {}", booking.getId());
        } catch (Exception e) {
            log.error("❌ Failed to publish BOOKING_CREATED", e);
        }
    }

    // ─── BOOKING CONFIRMED ────────────────────────────────────────────────────

    public void publishBookingConfirmed(Booking booking) {
        try {
            // ✅ Resolve startTime and duration
            // For direct bookings: use sessionStartTime + durationMinutes
            // For batch bookings: use first slot in sessions array
            LocalDateTime startTime = booking.getSessionStartTime();
            LocalDateTime endTime   = booking.getSessionEndTime();
            Integer duration        = booking.getDurationMinutes();

            if ((startTime == null || duration == null || duration == 0)
                    && booking.getSessions() != null
                    && !booking.getSessions().isEmpty()) {

                Booking.SessionTime first = booking.getSessions().get(0);
                startTime = first.getStartTime();
                endTime   = first.getEndTime();
                duration  = (int) java.time.Duration
                        .between(first.getStartTime(), first.getEndTime())
                        .toMinutes();

                log.info("📅 Batch booking - using first slot: {} to {}, duration: {} min",
                        startTime, endTime, duration);
            }

            if (startTime == null) {
                log.error("❌ Cannot publish BOOKING_CONFIRMED - startTime is null for booking: {}",
                        booking.getId());
                return;
            }

            if (duration == null || duration == 0) {
                log.error("❌ Cannot publish BOOKING_CONFIRMED - duration is 0 for booking: {}",
                        booking.getId());
                return;
            }

            // ✅ classSessionId = booking.getSessionId()
            // (set during createDirectTeacherBooking or confirmBooking for batch)
            String classSessionId = booking.getSessionId();
            if (classSessionId == null || classSessionId.isBlank()) {
                log.error("❌ Cannot publish BOOKING_CONFIRMED - classSessionId is null for booking: {}",
                        booking.getId());
                return;
            }

            Map<String, Object> event = new HashMap<>();
            event.put("eventType",          "BOOKING_CONFIRMED");
            event.put("bookingId",          booking.getId());
            event.put("classSessionId",     classSessionId);
            event.put("teacherId",          booking.getTeacherId());
            event.put("studentId",          booking.getStudentId());
            event.put("parentId",           booking.getParentId() != null
                    ? booking.getParentId() : "");
            event.put("scheduledStartTime", startTime.toString());
            event.put("scheduledEndTime",   endTime != null ? endTime.toString() : "");
            event.put("durationMinutes",    duration);
            event.put("subject",            booking.getSubject() != null
                    ? booking.getSubject() : "One-on-One Class");
            event.put("timestamp",          java.time.Instant.now().toString());

            kafkaTemplate.send(TOPIC, booking.getId(), event);

            log.info("✅ Published BOOKING_CONFIRMED for booking: {}", booking.getId());
            log.info("   classSessionId : {}", classSessionId);
            log.info("   teacherId      : {}", booking.getTeacherId());
            log.info("   studentId      : {}", booking.getStudentId());
            log.info("   startTime      : {}", startTime);
            log.info("   durationMinutes: {}", duration);

        } catch (Exception e) {
            log.error("❌ Failed to publish BOOKING_CONFIRMED", e);
        }
    }

    // ─── BOOKING APPROVED ─────────────────────────────────────────────────────

    public void publishBookingApproved(Booking booking) {
        try {
            // ✅ FIX: Use Map<String, Object> like publishBookingConfirmed
            // BookingEvent uses "sessionId" but listener reads "classSessionId"

            LocalDateTime startTime = booking.getSessionStartTime();
            LocalDateTime endTime   = booking.getSessionEndTime();
            Integer duration        = booking.getDurationMinutes();

            // Handle batch bookings
            if ((startTime == null || duration == null || duration == 0)
                    && booking.getSessions() != null
                    && !booking.getSessions().isEmpty()) {
                Booking.SessionTime first = booking.getSessions().get(0);
                startTime = first.getStartTime();
                endTime   = first.getEndTime();
                duration  = (int) java.time.Duration
                        .between(first.getStartTime(), first.getEndTime())
                        .toMinutes();
            }

            // ✅ classSessionId = booking.getSessionId()
            String classSessionId = booking.getSessionId();

            Map<String, Object> event = new HashMap<>();
            event.put("eventType",          "BOOKING_APPROVED");
            event.put("bookingId",          booking.getId());
            event.put("classSessionId",     classSessionId);      // ✅ correct field name
            event.put("sessionId",          classSessionId);      // ✅ also send as sessionId for fallback
            event.put("teacherId",          booking.getTeacherId());
            event.put("studentId",          booking.getStudentId());
            event.put("parentId",           booking.getParentId() != null
                    ? booking.getParentId() : "");
            event.put("scheduledStartTime", startTime != null ? startTime.toString() : "");
            event.put("scheduledEndTime",   endTime   != null ? endTime.toString()   : "");
            event.put("durationMinutes",    duration  != null ? duration             : 60);
            event.put("subject",            booking.getSubject() != null
                    ? booking.getSubject() : "One-on-One Class");
            event.put("timestamp",          java.time.Instant.now().toString());

            kafkaTemplate.send(TOPIC, booking.getId(), event);

            log.info("📤 Published BOOKING_APPROVED for booking: {}", booking.getId());
            log.info("   classSessionId : {}", classSessionId);
            log.info("   teacherId      : {}", booking.getTeacherId());
            log.info("   studentId      : {}", booking.getStudentId());
            log.info("   startTime      : {}", startTime);
            log.info("   durationMinutes: {}", duration);

        } catch (Exception e) {
            log.error("❌ Failed to publish BOOKING_APPROVED", e);
        }
    }

    // ─── BOOKING REJECTED ─────────────────────────────────────────────────────

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
            log.info("📤 Published BOOKING_REJECTED for booking: {}", booking.getId());
        } catch (Exception e) {
            log.error("❌ Failed to publish BOOKING_REJECTED", e);
        }
    }

    // ─── BOOKING CANCELLED ────────────────────────────────────────────────────

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
            log.info("📤 Published BOOKING_CANCELLED for booking: {}", booking.getId());
        } catch (Exception e) {
            log.error("❌ Failed to publish BOOKING_CANCELLED", e);
        }
    }

    // ─── EVENT DTO ────────────────────────────────────────────────────────────

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
        private BigDecimal refundAmount;
        private LocalDateTime timestamp;
    }
}