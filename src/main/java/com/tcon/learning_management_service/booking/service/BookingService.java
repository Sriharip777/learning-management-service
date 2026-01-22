package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.booking.dto.BookingDto;
import com.tcon.learning_management_service.booking.dto.BookingRequest;
import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.entity.CancellationPolicy;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.event.BookingEventPublisher;
import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ClassSessionRepository sessionRepository;
    private final BookingEventPublisher eventPublisher;
    private final BookingLockService lockService;

    @Transactional
    public BookingDto createBooking(String studentId, BookingRequest request) {
        log.info("Creating booking for student {} and session {}", studentId, request.getSessionId());

        // Get session
        ClassSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        // Validate session status
        if (session.getStatus() != ClassStatus.SCHEDULED) {
            throw new IllegalArgumentException("Session is not available for booking");
        }

        // Check if session is in the future
        if (session.getScheduledStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book past sessions");
        }

        // Check if already booked
        if (bookingRepository.existsBySessionIdAndStudentId(request.getSessionId(), studentId)) {
            throw new IllegalArgumentException("Student has already booked this session");
        }

        // Check capacity using lock
        String lockKey = "session:" + request.getSessionId();
        if (!lockService.acquireLock(lockKey, studentId)) {
            throw new IllegalArgumentException("Session is currently being booked by another user");
        }

        try {
            Long confirmedBookings = bookingRepository.countBySessionIdAndStatus(
                    request.getSessionId(), BookingStatus.CONFIRMED);

            if (session.getMaxParticipants() != null && confirmedBookings >= session.getMaxParticipants()) {
                throw new IllegalArgumentException("Session is full");
            }

            // Create booking
            Booking booking = Booking.builder()
                    .sessionId(request.getSessionId())
                    .courseId(session.getCourseId())
                    .studentId(studentId)
                    .studentName(request.getStudentName())
                    .studentEmail(request.getStudentEmail())
                    .teacherId(session.getTeacherId())
                    .status(BookingStatus.PENDING)
                    .sessionStartTime(session.getScheduledStartTime())
                    .sessionEndTime(session.getScheduledEndTime())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .bookedAt(LocalDateTime.now())
                    .cancellationPolicy(getDefaultCancellationPolicy())
                    .reminderSent(false)
                    .notes(request.getNotes())
                    .build();

            Booking saved = bookingRepository.save(booking);
            log.info("Booking created successfully: {}", saved.getId());

            // Publish event
            eventPublisher.publishBookingCreated(saved);

            return toDto(saved);
        } finally {
            lockService.releaseLock(lockKey, studentId);
        }
    }

    @Transactional
    public BookingDto confirmBooking(String bookingId, String paymentId, String transactionId) {
        log.info("Confirming booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking.setTransactionId(transactionId);
        booking.setConfirmedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("Booking confirmed: {}", bookingId);

        // Publish event
        eventPublisher.publishBookingConfirmed(updated);

        return toDto(updated);
    }

    public BookingDto getBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));
        return toDto(booking);
    }

    public List<BookingDto> getStudentBookings(String studentId) {
        return bookingRepository.findByStudentId(studentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getTeacherBookings(String teacherId) {
        return bookingRepository.findByTeacherId(teacherId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getSessionBookings(String sessionId) {
        return bookingRepository.findBySessionId(sessionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getStudentUpcomingBookings(String studentId) {
        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findByStudentIdAndSessionStartTimeBetween(
                        studentId, now, now.plusMonths(1))
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingDto completeBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("Booking completed: {}", bookingId);

        return toDto(updated);
    }

    private CancellationPolicy getDefaultCancellationPolicy() {
        return CancellationPolicy.builder()
                .hoursBeforeSession(24)
                .refundPercentage(100)
                .policyDescription("Full refund if cancelled 24 hours before session")
                .build();
    }

    private BookingDto toDto(Booking booking) {
        return BookingDto.builder()
                .id(booking.getId())
                .sessionId(booking.getSessionId())
                .courseId(booking.getCourseId())
                .studentId(booking.getStudentId())
                .studentName(booking.getStudentName())
                .studentEmail(booking.getStudentEmail())
                .teacherId(booking.getTeacherId())
                .status(booking.getStatus())
                .sessionStartTime(booking.getSessionStartTime())
                .sessionEndTime(booking.getSessionEndTime())
                .amount(booking.getAmount())
                .currency(booking.getCurrency())
                .paymentId(booking.getPaymentId())
                .transactionId(booking.getTransactionId())
                .bookedAt(booking.getBookedAt())
                .confirmedAt(booking.getConfirmedAt())
                .cancelledAt(booking.getCancelledAt())
                .completedAt(booking.getCompletedAt())
                .cancellationReason(booking.getCancellationReason())
                .cancelledBy(booking.getCancelledBy())
                .cancellationPolicy(booking.getCancellationPolicy())
                .refundAmount(booking.getRefundAmount())
                .refundTransactionId(booking.getRefundTransactionId())
                .refundedAt(booking.getRefundedAt())
                .reminderSent(booking.getReminderSent())
                .notes(booking.getNotes())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
