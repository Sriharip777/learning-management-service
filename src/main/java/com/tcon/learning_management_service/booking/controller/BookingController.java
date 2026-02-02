package com.tcon.learning_management_service.booking.controller;

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

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(
            @RequestHeader("X-User-Id") String studentId,
            @Valid @RequestBody BookingRequest request) {
        log.info("Creating booking for student: {}", studentId);
        BookingDto booking = bookingService.createBooking(studentId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @PostMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingDto> confirmBooking(
            @PathVariable String bookingId,
            @RequestBody Map<String, String> paymentData) {
        String paymentId = paymentData.get("paymentId");
        String transactionId = paymentData.get("transactionId");
        BookingDto booking = bookingService.confirmBooking(bookingId, paymentId, transactionId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingDto> getBooking(@PathVariable String bookingId) {
        BookingDto booking = bookingService.getBooking(bookingId);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<BookingDto>> getStudentBookings(@PathVariable String studentId) {
        List<BookingDto> bookings = bookingService.getStudentBookings(studentId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<List<BookingDto>> getTeacherBookings(@PathVariable String teacherId) {
        List<BookingDto> bookings = bookingService.getTeacherBookings(teacherId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<BookingDto>> getSessionBookings(@PathVariable String sessionId) {
        List<BookingDto> bookings = bookingService.getSessionBookings(sessionId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/student/{studentId}/upcoming")
    public ResponseEntity<List<BookingDto>> getStudentUpcomingBookings(@PathVariable String studentId) {
        List<BookingDto> bookings = bookingService.getStudentUpcomingBookings(studentId);
        return ResponseEntity.ok(bookings);
    }

    @PostMapping("/{bookingId}/cancel")
    public ResponseEntity<Map<String, Object>> cancelBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody BookingCancellationRequest request) {
        BigDecimal refundAmount = cancellationService.cancelBooking(bookingId, userId, request.getReason());
        return ResponseEntity.ok(Map.of(
                "message", "Booking cancelled successfully",
                "refundAmount", refundAmount
        ));
    }

    @GetMapping("/availability/teacher/{teacherId}")
    public ResponseEntity<?> getTeacherAvailability(
            @PathVariable String teacherId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        var availability = availabilityService.getTeacherAvailability(teacherId, start, end);
        return ResponseEntity.ok(availability);
    }

    // ==================== ADD TO BookingController.java ====================

    @PostMapping("/{bookingId}/approve")
    public ResponseEntity<BookingDto> approveBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody Map<String, String> requestBody) {

        String teacherMessage = requestBody.get("message");
        BookingDto booking = bookingService.approveBooking(bookingId, teacherId, teacherMessage);
        return ResponseEntity.ok(booking);
    }

    @PostMapping("/{bookingId}/reject")
    public ResponseEntity<BookingDto> rejectBooking(
            @PathVariable String bookingId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestBody Map<String, String> requestBody) {

        String rejectionReason = requestBody.get("reason");
        BookingDto booking = bookingService.rejectBooking(bookingId, teacherId, rejectionReason);
        return ResponseEntity.ok(booking);
    }

    @GetMapping("/teacher/{teacherId}/pending")
    public ResponseEntity<List<BookingDto>> getTeacherPendingRequests(@PathVariable String teacherId) {
        List<BookingDto> requests = bookingService.getTeacherPendingRequests(teacherId);
        return ResponseEntity.ok(requests);
    }
}
