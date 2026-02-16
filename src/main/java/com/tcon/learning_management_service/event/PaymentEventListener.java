package com.tcon.learning_management_service.event;

import com.tcon.learning_management_service.booking.service.BookingService;
import com.tcon.learning_management_service.course.service.CourseEnrollmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final BookingService bookingService;
    private final CourseEnrollmentService enrollmentService;

    @KafkaListener(
            topics = "payment-completed",
            groupId = "learning-management-service"
    )
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info("📨 ============ PAYMENT COMPLETED EVENT RECEIVED ============");
        log.info("📦 Raw Event: {}", event);

        try {
            String bookingId = (String) event.get("bookingId");
            String courseId = (String) event.get("courseId");
            String paymentId = (String) event.get("paymentId");
            String studentId = (String) event.get("studentId");

            log.info("🎫 Booking ID: {}", bookingId);
            log.info("🎓 Course ID: {}", courseId);
            log.info("💰 Payment ID: {}", paymentId);

            // ✅ CASE 1: One-on-One Booking Payment
            if (bookingId != null && !bookingId.isEmpty()) {
                log.info("📋 Processing ONE-ON-ONE booking payment");
                bookingService.confirmBooking(bookingId, paymentId, paymentId);
                log.info("✅ Booking confirmed successfully: {}", bookingId);
            }

            // ✅ CASE 2: Course Enrollment Payment
            else if (courseId != null && !courseId.isEmpty()) {
                log.info("🎓 Processing COURSE ENROLLMENT payment");

                // Get student info from event
                String studentName = (String) event.get("studentName");
                String studentEmail = (String) event.get("studentEmail");

                // If not in event, use IDs as fallback
                if (studentName == null) studentName = "Student " + studentId;
                if (studentEmail == null) studentEmail = studentId + "@example.com";

                // Get payment amount
                Object amountObj = event.get("amount");
                BigDecimal amountPaid = amountObj != null ?
                        new BigDecimal(amountObj.toString()) : BigDecimal.ZERO;

                // Enroll student in course
                enrollmentService.enrollStudent(
                        courseId,
                        studentId,
                        studentName,
                        studentEmail,
                        paymentId,
                        amountPaid
                );

                log.info("✅ Student enrolled in course successfully: {}", courseId);
            }

            else {
                log.error("❌ Neither bookingId nor courseId found in event");
            }

        } catch (Exception e) {
            log.error("❌ Failed to process payment completed event", e);
            log.error("❌ Event data: {}", event);
        }

        log.info("📨 ============ EVENT PROCESSING COMPLETE ============");
    }

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