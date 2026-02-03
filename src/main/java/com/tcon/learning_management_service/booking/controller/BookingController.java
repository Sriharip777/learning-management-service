package com.tcon.learning_management_service.booking.controller;

import com.tcon.learning_management_service.booking.dto.AvailabilityDto;
import com.tcon.learning_management_service.booking.dto.BookingCancellationRequest;
import com.tcon.learning_management_service.booking.dto.BookingDto;
import com.tcon.learning_management_service.booking.dto.BookingRequest;
import com.tcon.learning_management_service.booking.service.AvailabilityService;
import com.tcon.learning_management_service.booking.service.BookingService;
import com.tcon.learning_management_service.booking.service.CancellationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.support.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final CancellationService cancellationService;
    private final AvailabilityService availabilityService;

    // ==================== CREATE BOOKING ====================

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody BookingRequest request) {

        log.info("📥 POST /api/bookings - Creating booking");
        log.info("🆔 User ID from header: {}", userId);

        // ⭐ DETAILED LOGGING
        log.info("📋 Request object: {}", request);
        log.info("  - teacherId: {}", request.getTeacherId());
        log.info("  - sessionStartTime: {}", request.getSessionStartTime());
        log.info("  - sessionEndTime: {}", request.getSessionEndTime());
        log.info("  - sessionId: {}", request.getSessionId());
        log.info("  - subject: {}", request.getSubject());
        log.info("  - notes: {}", request.getNotes());

        try {
            BookingDto booking = bookingService.createBooking(userId, request);

            log.info("✅ Booking created successfully: ID={}, Status={}",
                    booking.getId(), booking.getStatus());

            return ResponseEntity.status(HttpStatus.CREATED).body(booking);

        } catch (IllegalArgumentException e) {
            log.error("❌ Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body(
                    Map.of("error", e.getMessage())
            );

        } catch (Exception e) {
            log.error("❌ Unexpected error creating booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to create booking: " + e.getMessage())
            );
        }
    }
    // ==================== CONFIRM BOOKING (AFTER PAYMENT) ====================

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<?> confirmBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, String> paymentData) {

        log.info("📥 POST /api/bookings/{}/confirm", bookingId);
        log.info("Payment data: {}", paymentData);

        try {
            String paymentId = paymentData.get("paymentId");
            String transactionId = paymentData.get("transactionId");

            BookingDto booking = bookingService.confirmBooking(bookingId, paymentId, transactionId);
            log.info("✅ Booking confirmed: {}", bookingId);

            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            log.error("❌ Error confirming booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", "Failed to confirm booking: " + e.getMessage())
            );
        }
    }

    // ==================== GET BOOKING BY ID ====================

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBooking(@PathVariable String bookingId) {
        log.info("📥 GET /api/bookings/{}", bookingId);

        try {
            BookingDto booking = bookingService.getBooking(bookingId);
            return ResponseEntity.ok(booking);

        } catch (IllegalArgumentException e) {
            log.error("❌ Booking not found: {}", bookingId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    Map.of("error", "Booking not found: " + bookingId)
            );

        } catch (Exception e) {
            log.error("❌ Error fetching booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ==================== GET BOOKINGS BY STUDENT ====================

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<BookingDto>> getStudentBookings(@PathVariable String studentId) {
        log.info("📥 GET /api/bookings/student/{}", studentId);

        List<BookingDto> bookings = bookingService.getStudentBookings(studentId);
        log.info("✅ Found {} bookings for student {}", bookings.size(), studentId);

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/student/{studentId}/upcoming")
    public ResponseEntity<List<BookingDto>> getStudentUpcomingBookings(@PathVariable String studentId) {
        log.info("📥 GET /api/bookings/student/{}/upcoming", studentId);

        List<BookingDto> bookings = bookingService.getStudentUpcomingBookings(studentId);
        log.info("✅ Found {} upcoming bookings for student {}", bookings.size(), studentId);

        return ResponseEntity.ok(bookings);
    }

    // ==================== GET BOOKINGS BY TEACHER ====================

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<BookingDto>> getTeacherBookings(@PathVariable String teacherId) {
        log.info("📥 GET /api/bookings/teacher/{}", teacherId);

        List<BookingDto> bookings = bookingService.getTeacherBookings(teacherId);
        log.info("✅ Found {} bookings for teacher {}", bookings.size(), teacherId);

        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/teacher/{teacherId}/pending")
    public ResponseEntity<List<BookingDto>> getTeacherPendingRequests(@PathVariable String teacherId) {
        log.info("📥 GET /api/bookings/teacher/{}/pending", teacherId);

        List<BookingDto> requests = bookingService.getTeacherPendingRequests(teacherId);
        log.info("✅ Found {} pending requests for teacher {}", requests.size(), teacherId);

        return ResponseEntity.ok(requests);
    }

    // ==================== GET BOOKINGS BY SESSION ====================

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<BookingDto>> getSessionBookings(@PathVariable String sessionId) {
        log.info("📥 GET /api/bookings/session/{}", sessionId);

        List<BookingDto> bookings = bookingService.getSessionBookings(sessionId);
        log.info("✅ Found {} bookings for session {}", bookings.size(), sessionId);

        return ResponseEntity.ok(bookings);
    }

    // ==================== TEACHER APPROVE/REJECT ====================

    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<?> approveBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody Map<String, String> requestBody) {

        log.info("📥 POST /api/bookings/{}/approve - Teacher: {}", bookingId, teacherId);

        try {
            String teacherMessage = requestBody.get("message");
            BookingDto booking = bookingService.approveBooking(bookingId, teacherId, teacherMessage);

            log.info("✅ Booking approved: {}", bookingId);
            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            log.error("❌ Error approving booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<?> rejectBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody Map<String, String> requestBody) {

        log.info("📥 POST /api/bookings/{}/reject - Teacher: {}", bookingId, teacherId);

        try {
            String rejectionReason = requestBody.get("reason");
            BookingDto booking = bookingService.rejectBooking(bookingId, teacherId, rejectionReason);

            log.info("✅ Booking rejected: {}", bookingId);
            return ResponseEntity.ok(booking);

        } catch (Exception e) {
            log.error("❌ Error rejecting booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ==================== CANCEL BOOKING ====================

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<?> cancelBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody BookingCancellationRequest request) {

        log.info("📥 POST /api/bookings/{}/cancel - User: {}", bookingId, userId);
        log.info("Cancellation reason: {}", request.getReason());

        try {
            BigDecimal refundAmount = cancellationService.cancelBooking(bookingId, userId, request.getReason());

            log.info("✅ Booking cancelled: {}, Refund: {}", bookingId, refundAmount);

            return ResponseEntity.ok(Map.of(
                    "message", "Booking cancelled successfully",
                    "refundAmount", refundAmount
            ));

        } catch (Exception e) {
            log.error("❌ Error cancelling booking", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }

    // ==================== GET TEACHER AVAILABILITY ====================

    @GetMapping("/availability/teacher/{teacherId}")
    public ResponseEntity<?> getTeacherAvailability(
            @PathVariable String teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {

        log.info("📥 GET /api/bookings/availability/teacher/{}", teacherId);
        log.info("Date range: {} to {}", start, end);

        try {
            List<AvailabilityDto> availability = availabilityService.getTeacherAvailability(teacherId, start, end);

            log.info("✅ Found {} availability slots for teacher {}", availability.size(), teacherId);

            return ResponseEntity.ok(availability);

        } catch (Exception e) {
            log.error("❌ Error fetching availability", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    Map.of("error", e.getMessage())
            );
        }
    }
}