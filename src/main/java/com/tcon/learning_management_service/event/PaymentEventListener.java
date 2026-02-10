package com.tcon.learning_management_service.event;

import com.tcon.learning_management_service.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingService bookingService;

    // ✅ Listen to the correct topic: payment-completed
    @KafkaListener(
            topics = "payment-completed",
            groupId = "learning-management-service"
    )
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info("📨 ============ PAYMENT COMPLETED EVENT RECEIVED ============");
        log.info("📦 Raw Event: {}", event);

        try {
            String bookingId = (String) event.get("bookingId");
            String paymentId = (String) event.get("paymentId");

            log.info("🎫 Booking ID: {}", bookingId);
            log.info("💰 Payment ID: {}", paymentId);

            if (bookingId == null || bookingId.isEmpty()) {
                log.error("❌ Booking ID is missing in event");
                return;
            }

            // Confirm the booking
            bookingService.confirmBooking(bookingId, paymentId, paymentId);

            log.info("✅ Booking confirmed successfully: {}", bookingId);

        } catch (Exception e) {
            log.error("❌ Failed to process payment completed event", e);
            log.error("❌ Event data: {}", event);
        }

        log.info("📨 ============ EVENT PROCESSING COMPLETE ============");
    }

    // Keep your existing listeners for other payment events
    @KafkaListener(topics = "payment-failed", groupId = "learning-management-service")
    public void handlePaymentFailed(Map<String, Object> event) {
        log.info("📨 Received payment failed event: {}", event);

        String bookingId = (String) event.get("bookingId");
        String reason = (String) event.get("failureReason");

        log.info("❌ Payment failed for booking: {}, Reason: {}", bookingId, reason);
        // TODO: Update booking status to PAYMENT_FAILED
    }

    @KafkaListener(topics = "refund-completed", groupId = "learning-management-service")
    public void handleRefundProcessed(Map<String, Object> event) {
        log.info("📨 Received refund completed event: {}", event);

        String bookingId = (String) event.get("bookingId");

        log.info("💸 Refund processed for booking: {}", bookingId);
        // TODO: Update booking status
    }
}
