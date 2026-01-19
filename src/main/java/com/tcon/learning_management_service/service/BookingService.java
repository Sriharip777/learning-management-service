package com.tcon.learning_management_service.service;

import com.tcon.learning_management_service.dto.BookingDto;
import com.tcon.learning_management_service.dto.BookingRequest;
import com.tcon.learning_management_service.entity.Booking;
import com.tcon.learning_management_service.entity.BookingStatus;
import com.tcon.learning_management_service.entity.Course;
import com.tcon.learning_management_service.event.BookingEventPublisher;
import com.tcon.learning_management_service.repository.BookingRepository;
import com.tcon.learning_management_service.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CourseRepository courseRepository;
    private final BookingEventPublisher bookingEventPublisher;
    private final AvailabilityService availabilityService;

    public BookingDto createBooking(BookingRequest request) {
        log.info("Creating booking for course: {}", request.getCourseId());

        // Check availability
        if (!availabilityService.isTeacherAvailable(
                request.getTeacherId(), request.getStartTime(), request.getEndTime())) {
            throw new RuntimeException("Teacher is not available at the requested time");
        }

        // Get course details
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        Booking booking = Booking.builder()
                .courseId(request.getCourseId())
                .studentId(request.getStudentId())
                .teacherId(request.getTeacherId())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(BookingStatus.PENDING)
                .amount(course.getPrice())
                .currency(course.getCurrency())
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // Publish booking created event
        bookingEventPublisher.publishBookingCreated(savedBooking);

        return convertToDto(savedBooking);
    }

    public BookingDto confirmBooking(String bookingId, String paymentId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);

        Booking confirmedBooking = bookingRepository.save(booking);

        // Publish booking confirmed event
        bookingEventPublisher.publishBookingConfirmed(confirmedBooking);

        return convertToDto(confirmedBooking);
    }

    public BookingDto cancelBooking(String bookingId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancellationReason(reason);

        Booking cancelledBooking = bookingRepository.save(booking);

        // Publish booking cancelled event
        bookingEventPublisher.publishBookingCancelled(cancelledBooking);

        return convertToDto(cancelledBooking);
    }

    public List<BookingDto> getStudentBookings(String studentId) {
        return bookingRepository.findByStudentId(studentId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getTeacherBookings(String teacherId) {
        return bookingRepository.findByTeacherId(teacherId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private BookingDto convertToDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .courseId(booking.getCourseId())
                .studentId(booking.getStudentId())
                .teacherId(booking.getTeacherId())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .status(booking.getStatus())
                .amount(booking.getAmount())
                .currency(booking.getCurrency())
                .paymentId(booking.getPaymentId())
                .createdAt(booking.getCreatedAt())
                .build();
    }
}
