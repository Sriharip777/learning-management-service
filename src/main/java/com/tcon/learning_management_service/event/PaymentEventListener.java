package com.tcon.learning_management_service.event;


import com.tcon.learning_management_service.booking.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingService bookingService;

    @KafkaListener(topics = "payment-events", groupId = "learning-management-service")
    public void handlePaymentEvent(PaymentEvent event) {
        log.info("Received payment event: {} for booking: {}", event.getEventType(), event.getBookingId());

        switch (event.getEventType()) {
            case "PAYMENT_SUCCESS":
                handlePaymentSuccess(event);
                break;
            case "PAYMENT_FAILED":
                handlePaymentFailed(event);
                break;
            case "REFUND_PROCESSED":
                handleRefundProcessed(event);
                break;
            default:
                log.warn("Unknown payment event type: {}", event.getEventType());
        }
    }

    private void handlePaymentSuccess(PaymentEvent event) {
        try {
            log.info("Processing payment success for booking: {}", event.getBookingId());
            bookingService.confirmBooking(
                    event.getBookingId(),
                    event.getPaymentId(),
                    event.getTransactionId()
            );
            log.info("Booking confirmed after payment success: {}", event.getBookingId());
        } catch (Exception e) {
            log.error("Failed to confirm booking after payment success", e);
        }
    }

    private void handlePaymentFailed(PaymentEvent event) {
        log.info("Processing payment failure for booking: {}", event.getBookingId());
        // Add logic: Cancel booking
        // Add logic: Notify student
    }

    private void handleRefundProcessed(PaymentEvent event) {
        log.info("Processing refund for booking: {}", event.getBookingId());
        // Add logic: Update booking status
        // Add logic: Send refund confirmation
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PaymentEvent {
        private String eventType;
        private String bookingId;
        private String paymentId;
        private String transactionId;
        private java.math.BigDecimal amount;
        private String currency;
        private java.time.LocalDateTime timestamp;
    }
}
