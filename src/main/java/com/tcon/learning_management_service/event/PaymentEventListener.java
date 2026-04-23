package com.tcon.learning_management_service.event;

import com.tcon.learning_management_service.booking.dto.BookingDto;
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

    @KafkaListener(topics = "payment-completed", groupId = "learning-management-service")
    public void handlePaymentCompleted(Map<String, Object> event) {
        log.info("📨 ============ PAYMENT COMPLETED EVENT RECEIVED ============");
        log.info("📦 Raw Event: {}", event);

        try {
            String bookingId = event.get("bookingId") != null ? event.get("bookingId").toString() : null;
            String courseId = event.get("courseId") != null ? event.get("courseId").toString() : null;
            String paymentId = event.get("paymentId") != null ? event.get("paymentId").toString() : null;
            String studentId = event.get("studentId") != null ? event.get("studentId").toString() : null;

            String studentName = event.get("studentName") != null
                    ? event.get("studentName").toString()
                    : "Student " + studentId;

            String studentEmail = event.get("studentEmail") != null
                    ? event.get("studentEmail").toString()
                    : studentId + "@example.com";

            Object amountObj = event.get("amount");
            BigDecimal amountPaid = amountObj != null
                    ? new BigDecimal(amountObj.toString())
                    : BigDecimal.ZERO;

            log.info("🎫 Booking ID: {}", bookingId);
            log.info("🎓 Course ID: {}", courseId);
            log.info("👤 Student ID: {}", studentId);
            log.info("💰 Payment ID: {}", paymentId);

            // CASE 1: booking-backed payment
            if (bookingId != null && !bookingId.isBlank()) {
                log.info("📋 Processing booking payment");

                BookingDto booking = bookingService.confirmBooking(bookingId, paymentId, paymentId);
                log.info("✅ Booking confirmed successfully: {}", bookingId);

                if (booking.getCourseId() != null && !booking.getCourseId().isBlank()) {
                    log.info("🎓 Booking is linked to course {}, ensuring enrollment", booking.getCourseId());

                    // ✅ Derive sessionMode from booking sessions
                    String sessionMode = (booking.getSessions() != null && !booking.getSessions().isEmpty())
                            ? "ONE_ON_ONE"
                            : "GROUP";

                    log.info("📌 Session mode derived from booking: {}", sessionMode);

                    try {
                        enrollmentService.enrollStudent(
                                booking.getCourseId(),
                                booking.getStudentId(),
                                booking.getStudentName(),
                                booking.getStudentEmail(),
                                paymentId,
                                amountPaid,
                                sessionMode  // ✅ pass sessionMode
                        );
                        log.info("✅ Student enrolled successfully from booking flow with mode: {}", sessionMode);
                    } catch (IllegalArgumentException ex) {
                        if (ex.getMessage() != null && ex.getMessage().contains("already enrolled")) {
                            log.info("ℹ️ Student already enrolled in course {}, skipping", booking.getCourseId());
                        } else {
                            throw ex;
                        }
                    }
                } else {
                    log.info("ℹ️ Booking is not course-linked, no enrollment needed");
                }

                // CASE 2: direct course payment (no booking)
            } else if (courseId != null && !courseId.isBlank()) {
                log.info("🎓 Processing direct course enrollment payment");

                // ✅ Direct course payment = GROUP by default
                String sessionMode = "GROUP";

                try {
                    enrollmentService.enrollStudent(
                            courseId,
                            studentId,
                            studentName,
                            studentEmail,
                            paymentId,
                            amountPaid,
                            sessionMode  // ✅ pass sessionMode
                    );
                    log.info("✅ Student enrolled in course successfully: {}", courseId);
                } catch (IllegalArgumentException ex) {
                    if (ex.getMessage() != null && ex.getMessage().contains("already enrolled")) {
                        log.info("ℹ️ Student already enrolled in course {}, skipping", courseId);
                    } else {
                        throw ex;
                    }
                }

            } else {
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

        String bookingId = event.get("bookingId") != null ? event.get("bookingId").toString() : null;
        String reason = event.get("failureReason") != null ? event.get("failureReason").toString() : null;

        log.info("❌ Payment failed for booking: {}, Reason: {}", bookingId, reason);
    }

    @KafkaListener(topics = "refund-completed", groupId = "learning-management-service")
    public void handleRefundProcessed(Map<String, Object> event) {
        log.info("📨 Received refund completed event: {}", event);

        String bookingId = event.get("bookingId") != null ? event.get("bookingId").toString() : null;
        log.info("💸 Refund processed for booking: {}", bookingId);
    }
}