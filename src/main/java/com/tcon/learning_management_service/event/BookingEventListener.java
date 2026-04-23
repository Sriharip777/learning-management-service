package com.tcon.learning_management_service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class BookingEventListener {

    @KafkaListener(
            topics = "booking-events",
            groupId = "learning-management-service"
    )
    public void handleBookingEvent(Map<String, Object> payload) {
        if (payload == null) {
            log.warn("📥 Received null booking event payload");
            return;
        }

        log.info("📥 Received booking event payload: {}", payload);

        String eventType = (String) payload.get("eventType");
        String bookingId = (String) payload.get("bookingId");

        if (eventType == null) {
            log.warn("⚠️ Booking event without eventType, payload={}", payload);
            return;
        }

        switch (eventType) {
            case "BOOKING_CREATED"   -> handleBookingCreated(payload);
            case "BOOKING_CONFIRMED" -> handleBookingConfirmed(payload);
            case "BOOKING_APPROVED"  -> handleBookingApproved(payload);
            case "BOOKING_CANCELLED" -> handleBookingCancelled(payload);
            case "BOOKING_REJECTED"  -> handleBookingRejected(payload);
            default -> log.warn("⚠️ Unknown booking event type: {} for booking: {}", eventType, bookingId);
        }
    }

    private void handleBookingCreated(Map<String, Object> payload) {
        String bookingId  = (String) payload.get("bookingId");
        String studentId  = (String) payload.get("studentId");
        String teacherId  = (String) payload.get("teacherId");
        String sessionId  = (String) payload.getOrDefault("classSessionId", payload.get("sessionId"));

        log.info("🟢 BOOKING_CREATED: bookingId={}, sessionId={}, studentId={}, teacherId={}",
                bookingId, sessionId, studentId, teacherId);

        // TODO: Add your real logic here:
        //  - Send notification to teacher
        //  - Update analytics, etc.
    }

    private void handleBookingConfirmed(Map<String, Object> payload) {
        String bookingId      = (String) payload.get("bookingId");
        String classSessionId = (String) payload.getOrDefault("classSessionId", payload.get("sessionId"));

        log.info("🟢 BOOKING_CONFIRMED: bookingId={}, classSessionId={}",
                bookingId, classSessionId);

        // TODO: Add your logic:
        //  - Send confirmation email
        //  - Add to calendar
        //  - Send meeting details
    }

    private void handleBookingApproved(Map<String, Object> payload) {
        String bookingId      = (String) payload.get("bookingId");
        String classSessionId = (String) payload.getOrDefault("classSessionId", payload.get("sessionId"));

        log.info("🟢 BOOKING_APPROVED: bookingId={}, classSessionId={}",
                bookingId, classSessionId);

        // TODO: Add your logic for approved booking:
        //  - Notify teacher and student
        //  - Any other LMS updates
    }

    private void handleBookingCancelled(Map<String, Object> payload) {
        String bookingId          = (String) payload.get("bookingId");
        String cancellationReason = (String) payload.get("cancellationReason");

        log.info("🟡 BOOKING_CANCELLED: bookingId={}, reason={}",
                bookingId, cancellationReason);

        // TODO: Add your cancellation logic:
        //  - Send cancellation notification
        //  - Process refund (via financial-service)
        //  - Update teacher availability
    }

    private void handleBookingRejected(Map<String, Object> payload) {
        String bookingId          = (String) payload.get("bookingId");
        String cancellationReason = (String) payload.get("cancellationReason");

        log.info("🟡 BOOKING_REJECTED: bookingId={}, reason={}",
                bookingId, cancellationReason);

        // TODO: Add your rejection logic:
        //  - Notify student/parent
        //  - Update analytics, etc.
    }
}