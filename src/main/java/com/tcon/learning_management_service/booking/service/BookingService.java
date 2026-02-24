// src/main/java/com/tcon/learning_management_service/booking/service/BookingService.java

package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.booking.dto.BatchBookingRequest;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
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

    // ==================== CREATE BOOKING ====================

    @Transactional
    public BookingDto createBooking(String studentId, BookingRequest request) {
        log.info("📥 Creating booking for student: {}", studentId);
        log.info("📋 Request: {}", request);
        log.info("👤 Student info - Name: {}, Email: {}", request.getStudentName(), request.getStudentEmail());

        // ⭐ Validate student information (REQUIRED)
        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (request.getStudentEmail() == null || request.getStudentEmail().isBlank()) {
            throw new IllegalArgumentException("Student email is required");
        }

        // ⭐ CASE 1: Booking an existing session (has sessionId)
        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            return createBookingForExistingSession(studentId, request);
        }

        // ⭐ CASE 2: Creating a new booking request (direct teacher booking)
        else if (request.getTeacherId() != null && !request.getTeacherId().isEmpty()) {
            return createDirectTeacherBooking(studentId, request);
        }

        else {
            throw new IllegalArgumentException("Either sessionId or teacherId must be provided");
        }
    }

    /**
     * Create booking for an existing scheduled session
     */
    private BookingDto createBookingForExistingSession(String studentId, BookingRequest request) {
        log.info("📋 Creating booking for existing session: {}", request.getSessionId());

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
                    .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .bookedAt(LocalDateTime.now())
                    .cancellationPolicy(getDefaultCancellationPolicy())
                    .reminderSent(false)
                    .notes(request.getNotes())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            Booking saved = bookingRepository.save(booking);
            log.info("✅ Booking created: ID={}, Student={}, Session={}",
                    saved.getId(), saved.getStudentName(), saved.getSessionId());

            // Publish event
            eventPublisher.publishBookingCreated(saved);

            return toDto(saved);

        } finally {
            lockService.releaseLock(lockKey, studentId);
        }
    }

    /**
     * Create direct teacher booking request (creates session first)
     * Flow: Session → Booking → Event
     */
    private BookingDto createDirectTeacherBooking(String studentId, BookingRequest request) {
        log.info("🎯 Creating direct one-on-one booking for teacher: {}", request.getTeacherId());
        log.info("📅 Time: {} to {}", request.getSessionStartTime(), request.getSessionEndTime());

        // ==================== VALIDATION ====================

        if (request.getSessionStartTime() == null || request.getSessionEndTime() == null) {
            throw new IllegalArgumentException("Session start and end times are required");
        }

        if (request.getSessionStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot book sessions in the past");
        }

        if (request.getSessionEndTime().isBefore(request.getSessionStartTime())) {
            throw new IllegalArgumentException("Session end time must be after start time");
        }

        // ==================== CALCULATE DURATION ====================

        Integer duration = (int) java.time.Duration.between(
                request.getSessionStartTime(),
                request.getSessionEndTime()
        ).toMinutes();

        log.info("📏 Calculated duration: {} minutes", duration);

        // ==================== CHECK FOR CONFLICTS ====================

        List<ClassSession> overlapping = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                request.getTeacherId(),
                request.getSessionStartTime().minusMinutes(1),
                request.getSessionEndTime().plusMinutes(1)
        );

        if (!overlapping.isEmpty()) {
            log.warn("⚠️ Found {} overlapping booking(s), but creating as PENDING for teacher approval",
                    overlapping.size());
        }

        // ==================== STEP 1: CREATE SESSION FIRST ====================

        log.info("🆕 Creating ClassSession for one-on-one booking");

        ClassSession session = ClassSession.builder()
                .sessionType(com.tcon.learning_management_service.session.entity.SessionType.ONE_ON_ONE)
                .courseId(null) // One-on-one has no course
                .teacherId(request.getTeacherId())
                .teacherName("Teacher") // TODO: Fetch from User Service if needed
                .studentId(studentId) // ✅ NEW: Direct student reference
                .bookingId(null) // ✅ Will be set after booking is created
                .title(request.getSubject() != null ? request.getSubject() : "One-on-One Class")
                .description("Direct booking with " + request.getStudentName())
                .status(com.tcon.learning_management_service.session.entity.ClassStatus.SCHEDULED)
                .scheduledStartTime(request.getSessionStartTime())
                .scheduledEndTime(request.getSessionEndTime())
                .durationMinutes(duration)
                .maxParticipants(1) // One-on-one = single student
                .participants(new java.util.ArrayList<>())
                .attendedCount(0)
                .materialUrls(new java.util.ArrayList<>())
                .reminderSent(false)
                .createdBy(request.getTeacherId())
                .build();

        ClassSession savedSession = sessionRepository.save(session);
        log.info("✅ ClassSession created: {} (Type: ONE_ON_ONE)", savedSession.getId());

        // ==================== STEP 2: CREATE BOOKING LINKED TO SESSION ====================

        log.info("🔨 Creating Booking linked to session: {}", savedSession.getId());

        Booking booking = Booking.builder()
                .sessionId(savedSession.getId()) // ✅ ALWAYS has sessionId now
                .courseId(null) // One-on-one has no course
                .studentId(studentId)
                .studentName(request.getStudentName())
                .studentEmail(request.getStudentEmail())
                .teacherId(request.getTeacherId())
                .parentId(request.getParentId())
                .subject(request.getSubject())
                .durationMinutes(duration)
                .sessionStartTime(request.getSessionStartTime())
                .sessionEndTime(request.getSessionEndTime())
                .status(BookingStatus.PENDING) // Awaiting teacher approval
                .amount(request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .bookedAt(LocalDateTime.now())
                .cancellationPolicy(getDefaultCancellationPolicy())
                .reminderSent(false)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Booking saved = bookingRepository.save(booking);

        // ==================== STEP 3: UPDATE SESSION WITH BOOKING ID ====================

        savedSession.setBookingId(saved.getId());
        sessionRepository.save(savedSession);

        log.info("💾 Booking created successfully:");
        log.info("   📋 Booking ID: {}", saved.getId());
        log.info("   🎓 Session ID: {}", saved.getSessionId());
        log.info("   👤 Student: {} ({})", saved.getStudentName(), saved.getStudentEmail());
        log.info("   👨‍🏫 Teacher: {}", saved.getTeacherId());
        log.info("   📚 Subject: {}", saved.getSubject());
        log.info("   ⏱️ Duration: {} minutes", saved.getDurationMinutes());
        log.info("   📅 Time: {} to {}", saved.getSessionStartTime(), saved.getSessionEndTime());

        // Publish event
        eventPublisher.publishBookingCreated(saved);

        return toDto(saved);
    }

    // ==================== CREATE BATCH BOOKING (NEW) ====================

    /**
     * Create ONE booking with multiple sessions
     */
    @Transactional
    public BookingDto createBatchBooking(String studentId, BatchBookingRequest request) {
        log.info("📦 Creating multi-session booking for student: {}", studentId);
        log.info("  - Student: {} ({})", request.getStudentName(), request.getStudentEmail());
        log.info("  - Teacher: {}", request.getTeacherId());
        log.info("  - Sessions: {}", request.getSessions().size());
        log.info("  - Total amount: {} {}", request.getCurrency(), request.getTotalAmount());

        // ⭐ Validate
        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (request.getStudentEmail() == null || request.getStudentEmail().isBlank()) {
            throw new IllegalArgumentException("Student email is required");
        }
        if (request.getSessions() == null || request.getSessions().isEmpty()) {
            throw new IllegalArgumentException("At least one session is required");
        }

        // ⭐ Convert session slots to SessionTime entities
        List<Booking.SessionTime> sessionTimes = new ArrayList<>();
        for (BatchBookingRequest.SessionSlot slot : request.getSessions()) {
            if (slot.getSessionStartTime() == null || slot.getSessionEndTime() == null) {
                throw new IllegalArgumentException("Session start and end times are required");
            }
            if (slot.getSessionStartTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Cannot book sessions in the past");
            }
            if (slot.getSessionEndTime().isBefore(slot.getSessionStartTime())) {
                throw new IllegalArgumentException("Session end time must be after start time");
            }

            sessionTimes.add(Booking.SessionTime.builder()
                    .startTime(slot.getSessionStartTime())
                    .endTime(slot.getSessionEndTime())
                    .amount(slot.getAmount())
                    .build());
        }

        // ⭐ Create ONE booking with multiple sessions
        Booking booking = Booking.builder()
                .studentId(studentId)
                .studentName(request.getStudentName())
                .studentEmail(request.getStudentEmail())
                .teacherId(request.getTeacherId())
                .courseId(request.getCourseId())
                .sessions(sessionTimes) // ✅ All sessions in one booking
                .amount(request.getTotalAmount()) // ✅ Total amount for all sessions
                .currency(request.getCurrency())
                .status(BookingStatus.PENDING)
                .bookedAt(LocalDateTime.now())
                .cancellationPolicy(getDefaultCancellationPolicy())
                .reminderSent(false)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // ⭐ Save ONE booking
        Booking savedBooking = bookingRepository.save(booking);
        log.info("✅ Multi-session booking created: ID={}, Sessions={}, Total={}{}",
                savedBooking.getId(),
                sessionTimes.size(),
                request.getCurrency(),
                request.getTotalAmount());

        // ⭐ Publish event
        eventPublisher.publishBookingCreated(savedBooking);

        return toDto(savedBooking);
    }

    // ==================== CONFIRM BOOKING (AFTER PAYMENT) ====================

    @Transactional
    public BookingDto confirmBooking(String bookingId, String paymentId, String transactionId) {
        log.info("💳 Confirming booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        // ✅ ADD THIS LOG
        log.info("📊 Booking durationMinutes BEFORE confirm: {}", booking.getDurationMinutes());
        log.info("📅 Session times: {} to {}", booking.getSessionStartTime(), booking.getSessionEndTime());

        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Only pending bookings can be confirmed");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking.setTransactionId(transactionId);
        booking.setConfirmedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);

        // ✅ ADD THIS LOG
        log.info("📊 Booking durationMinutes AFTER save: {}", updated.getDurationMinutes());
        log.info("✅ Booking confirmed: {}", bookingId);

        // Publish event
        eventPublisher.publishBookingConfirmed(updated);

        // TODO: Create class sessions after payment
        // createSessionsFromBooking(bookingId);

        return toDto(updated);
    }

    // ==================== TEACHER APPROVE/REJECT ====================

    @Transactional
    public BookingDto approveBooking(String bookingId, String teacherId, String teacherMessage) {
        log.info("👍 Teacher {} approving booking {}", teacherId, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        // Validate teacher owns this booking
        if (!booking.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this booking");
        }

        // Validate status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be approved");
        }

        // Update booking status to PENDING_PAYMENT (student needs to pay)
        booking.setStatus(BookingStatus.PENDING_PAYMENT);

        // Append teacher's message to notes
        if (teacherMessage != null && !teacherMessage.isEmpty()) {
            String existingNotes = booking.getNotes() != null ? booking.getNotes() : "";
            booking.setNotes(existingNotes + (existingNotes.isEmpty() ? "" : "\n\n") +
                    "Teacher's message: " + teacherMessage);
        }

        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking approved: {} - Student: {}", bookingId, booking.getStudentName());

        // Publish event
        eventPublisher.publishBookingApproved(updated);

        return toDto(updated);
    }

    @Transactional
    public BookingDto rejectBooking(String bookingId, String teacherId, String rejectionReason) {
        log.info("👎 Teacher {} rejecting booking {}", teacherId, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        // Validate teacher owns this booking
        if (!booking.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this booking");
        }

        // Validate status
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be rejected");
        }

        // Update booking
        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancellationReason(rejectionReason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(teacherId);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking rejected: {} - Reason: {}", bookingId, rejectionReason);

        // Publish event
        eventPublisher.publishBookingRejected(updated);

        return toDto(updated);
    }

    // ==================== GET BOOKINGS ====================

    public BookingDto getBooking(String bookingId) {
        log.info("📋 Getting booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        return toDto(booking);
    }

    public List<BookingDto> getStudentBookings(String studentId) {
        log.info("📋 Getting bookings for student: {}", studentId);

        List<Booking> bookings = bookingRepository.findByStudentId(studentId);
        log.info("✅ Found {} bookings for student", bookings.size());

        return bookings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getTeacherBookings(String teacherId) {
        log.info("📋 Getting bookings for teacher: {}", teacherId);

        List<Booking> bookings = bookingRepository.findByTeacherId(teacherId);
        log.info("✅ Found {} bookings for teacher", bookings.size());

        return bookings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getSessionBookings(String sessionId) {
        log.info("📋 Getting bookings for session: {}", sessionId);

        return bookingRepository.findBySessionId(sessionId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getStudentUpcomingBookings(String studentId) {
        log.info("📋 Getting upcoming bookings for student: {}", studentId);

        LocalDateTime now = LocalDateTime.now();
        return bookingRepository.findByStudentIdAndSessionStartTimeBetween(
                        studentId, now, now.plusMonths(1))
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // src/main/java/com/tcon/learning_management_service/booking/service/BookingService.java

    public List<BookingDto> getParentBookings(String parentId) {
        log.info("📋 Getting bookings for parent: {}", parentId);

        List<Booking> bookings = bookingRepository.findByParentId(parentId);
        log.info("✅ Found {} bookings for parent {}", bookings.size(), parentId);

        return bookings.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getTeacherPendingRequests(String teacherId) {
        log.info("📋 Getting pending requests for teacher: {}", teacherId);

        List<Booking> pending = bookingRepository.findByTeacherIdAndStatus(
                teacherId, BookingStatus.PENDING);

        log.info("✅ Found {} pending requests for teacher", pending.size());

        // Log each pending request for debugging
        pending.forEach(booking -> {
            int sessionCount = (booking.getSessions() != null && !booking.getSessions().isEmpty())
                    ? booking.getSessions().size()
                    : 1;
            log.info("  📌 Pending: ID={}, Student={}, Email={}, Sessions={}, Amount={}",
                    booking.getId(),
                    booking.getStudentName(),
                    booking.getStudentEmail(),
                    sessionCount,
                    booking.getAmount());
        });

        return pending.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    // ==================== COMPLETE BOOKING ====================

    @Transactional
    public BookingDto completeBooking(String bookingId) {
        log.info("✅ Completing booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be completed");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking completed: {}", bookingId);

        return toDto(updated);
    }

    // ==================== CANCEL BOOKING ====================

    @Transactional
    public BookingDto cancelBooking(String bookingId, String userId, String reason) {
        log.info("❌ Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        // Validate user can cancel
        if (!booking.getStudentId().equals(userId) && !booking.getTeacherId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: Only student or teacher can cancel");
        }

        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.PENDING_PAYMENT &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot cancel booking in current status");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(userId);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking cancelled: {}", bookingId);

        return toDto(updated);
    }

    // ==================== HELPER METHODS ====================

    private CancellationPolicy getDefaultCancellationPolicy() {
        return CancellationPolicy.builder()
                .hoursBeforeSession(24)
                .refundPercentage(100)
                .policyDescription("Full refund if cancelled 24 hours before session")
                .build();
    }

    private BookingDto toDto(Booking booking) {
        // ✅ Convert sessions if present
        List<BookingDto.SessionTimeDto> sessionDtos = null;
        if (booking.getSessions() != null && !booking.getSessions().isEmpty()) {
            sessionDtos = booking.getSessions().stream()
                    .map(s -> BookingDto.SessionTimeDto.builder()
                            .startTime(s.getStartTime())
                            .endTime(s.getEndTime())
                            .amount(s.getAmount())
                            .build())
                    .collect(Collectors.toList());
        }

        return BookingDto.builder()
                .id(booking.getId())
                .sessionId(booking.getSessionId())
                .courseId(booking.getCourseId())
                .studentId(booking.getStudentId())
                .studentName(booking.getStudentName())
                .studentEmail(booking.getStudentEmail())
                .teacherId(booking.getTeacherId())
                .parentId(booking.getParentId())                    // ✅ ADD
                .subject(booking.getSubject())                      // ✅ ADD
                .durationMinutes(booking.getDurationMinutes())      // ✅ ADD
                .status(booking.getStatus())
                .sessionStartTime(booking.getSessionStartTime())
                .sessionEndTime(booking.getSessionEndTime())
                .sessions(sessionDtos) // ✅ Include sessions
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