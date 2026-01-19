package com.tcon.learning_management_service.controller;

import com.tcon.learning_management_service.dto.BookingDto;
import com.tcon.learning_management_service.dto.BookingRequest;
import com.tcon.learning_management_service.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingDto> createBooking(@Valid @RequestBody BookingRequest request) {
        BookingDto booking = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @PatchMapping("/{bookingId}/confirm")
    public ResponseEntity<BookingDto> confirmBooking(
            @PathVariable String bookingId,
            @RequestParam String paymentId) {
        BookingDto booking = bookingService.confirmBooking(bookingId, paymentId);
        return ResponseEntity.ok(booking);
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingDto> cancelBooking(
            @PathVariable String bookingId,
            @RequestParam String reason) {
        BookingDto booking = bookingService.cancelBooking(bookingId, reason);
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
}
