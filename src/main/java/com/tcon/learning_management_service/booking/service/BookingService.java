package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import com.tcon.learning_management_service.booking.dto.BatchBookingRequest;
import com.tcon.learning_management_service.booking.dto.BookingDto;
import com.tcon.learning_management_service.booking.dto.BookingRequest;
import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.entity.CancellationPolicy;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.client.VideoServiceClient;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateRequest;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateResponse;
import com.tcon.learning_management_service.demo.service.DemoLimitService;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
    private final DemoLimitService demoLimitService;
    private final VideoServiceClient videoServiceClient;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository; // ✅ NEW

    // ==================== CREATE BOOKING ====================

    @Transactional
    public BookingDto createBooking(String studentId, BookingRequest request) {
        log.info("📥 Creating booking for student: {}", studentId);
        log.info("📋 Request: {}", request);
        log.info("👤 Student info - Name: {}, Email: {}", request.getStudentName(), request.getStudentEmail());

        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (request.getStudentEmail() == null || request.getStudentEmail().isBlank()) {
            throw new IllegalArgumentException("Student email is required");
        }

        if (request.getSessionId() != null && !request.getSessionId().isEmpty()) {
            return createBookingForExistingSession(studentId, request);
        } else if (request.getTeacherId() != null && !request.getTeacherId().isEmpty()) {
            return createDirectTeacherBooking(studentId, request);
        } else {
            throw new IllegalArgumentException("Either sessionId or teacherId must be provided");
        }
    }

    /**
     * Create booking for an existing scheduled session
     */
    private BookingDto createBookingForExistingSession(String studentId, BookingRequest request) {
        log.info("📋 Creating booking for existing session: {}", request.getSessionId());

        ClassSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        if (session.getStatus() != ClassStatus.SCHEDULED) {
            throw new IllegalArgumentException("Session is not available for booking");
        }

        validateBookingTime(session.getScheduledStartTime());

        // ✅ OPTIONAL (recommended)

        LocalDateTime now = LocalDateTime.now();

        if (session.getScheduledStartTime().toLocalDate().equals(now.toLocalDate())) {

            if (session.getScheduledStartTime().isBefore(now.plusMinutes(30))) {
                throw new IllegalArgumentException(
                        "Too late to book this session (same-day buffer)"
                );
            }
        }

        if (bookingRepository.existsBySessionIdAndStudentId(request.getSessionId(), studentId)) {
            throw new IllegalArgumentException("Student has already booked this session");
        }

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

            boolean isFreeDemo = false;
            try {
                demoLimitService.consumeFreeDemo(studentId);
                isFreeDemo = true;
                log.info("Using free demo for student {} on session {}", studentId, request.getSessionId());
            } catch (Exception ex) {
                log.info("No free demo available for student {}: {}", studentId, ex.getMessage());
                isFreeDemo = false;
            }

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
                    .amount(isFreeDemo
                            ? BigDecimal.ZERO
                            : (request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO))
                    .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                    .bookedAt(LocalDateTime.now())
                    .cancellationPolicy(getDefaultCancellationPolicy())
                    .reminderSent(false)
                    .notes(request.getNotes())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .isFreeDemo(isFreeDemo)
                    .build();

            Booking saved = bookingRepository.save(booking);
            log.info("✅ Booking created: ID={}, Student={}, Session={}, isFreeDemo={}",
                    saved.getId(), saved.getStudentName(), saved.getSessionId(), saved.getIsFreeDemo());

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

        if (request.getSessionStartTime() == null || request.getSessionEndTime() == null) {
            throw new IllegalArgumentException("Session start and end times are required");
        }


        // ✅ ADD THIS (same-day support with buffer)

        if (request.getSessionEndTime().isBefore(request.getSessionStartTime())) {
            throw new IllegalArgumentException("Session end time must be after start time");
        }

        Integer duration = (int) java.time.Duration.between(
                request.getSessionStartTime(),
                request.getSessionEndTime()
        ).toMinutes();

        log.info("📏 Calculated duration: {} minutes", duration);

        List<ClassSession> overlapping = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                request.getTeacherId(),
                request.getSessionStartTime().minusMinutes(1),
                request.getSessionEndTime().plusMinutes(1)
        );

        // ✅ Time validation
        validateBookingTime(request.getSessionStartTime());

        if (!overlapping.isEmpty()) {
            throw new IllegalArgumentException("Time slot already booked for this teacher");
        }

// ✅ Availability check

        log.info("🆕 Creating ClassSession for one-on-one booking");

        ClassSession session = ClassSession.builder()
                .sessionType(com.tcon.learning_management_service.session.entity.SessionType.ONE_ON_ONE)
                .courseId(null)
                .teacherId(request.getTeacherId())
                .teacherName("Teacher")
                .studentId(studentId)
                .bookingId(null)
                .title(request.getSubject() != null ? request.getSubject() : "One-on-One Class")
                .description("Direct booking with " + request.getStudentName())
                .status(ClassStatus.SCHEDULED)
                .scheduledStartTime(request.getSessionStartTime())
                .scheduledEndTime(request.getSessionEndTime())
                .durationMinutes(duration)
                .maxParticipants(1)
                .participants(new java.util.ArrayList<>())
                .attendedCount(0)
                .materialUrls(new java.util.ArrayList<>())
                .reminderSent(false)
                .createdBy(request.getTeacherId())
                .build();

        ClassSession savedSession = sessionRepository.save(session);
        log.info("✅ ClassSession created: {} (Type: ONE_ON_ONE)", savedSession.getId());

        boolean isFreeDemo = false;
        try {
            demoLimitService.consumeFreeDemo(studentId);
            isFreeDemo = true;
            log.info("Using free demo for direct one-on-one booking, student {}", studentId);
        } catch (Exception ex) {
            log.info("No free demo available for student {}: {}", studentId, ex.getMessage());
            isFreeDemo = false;
        }

        log.info("🔨 Creating Booking linked to session: {}", savedSession.getId());

        Booking booking = Booking.builder()
                .sessionId(savedSession.getId())
                .courseId(null)
                .studentId(studentId)
                .studentName(request.getStudentName())
                .studentEmail(request.getStudentEmail())
                .teacherId(request.getTeacherId())
                .teacherName(savedSession.getTeacherName())
                .parentId(request.getParentId())
                .subject(request.getSubject())
                .durationMinutes(duration)
                .sessionStartTime(request.getSessionStartTime())
                .sessionEndTime(request.getSessionEndTime())
                .status(BookingStatus.PENDING)
                .amount(isFreeDemo
                        ? BigDecimal.ZERO
                        : (request.getAmount() != null ? request.getAmount() : BigDecimal.ZERO))
                .currency(request.getCurrency() != null ? request.getCurrency() : "INR")
                .bookedAt(LocalDateTime.now())
                .cancellationPolicy(getDefaultCancellationPolicy())
                .reminderSent(false)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isFreeDemo(isFreeDemo)
                .build();

        Booking saved = bookingRepository.save(booking);

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
        log.info("   🎁 isFreeDemo: {}", saved.getIsFreeDemo());

        eventPublisher.publishBookingCreated(saved);

        return toDto(saved);
    }

    // ==================== CREATE BATCH BOOKING (NEW) ====================

    @Transactional
    public BookingDto createBatchBooking(String studentId, BatchBookingRequest request) {
        log.info("📦 Creating multi-session booking for student: {}", studentId);
        log.info("  - Student: {} ({})", request.getStudentName(), request.getStudentEmail());
        log.info("  - Teacher: {}", request.getTeacherId());
        log.info("  - Sessions: {}", request.getSessions().size());
        log.info("  - Total amount: {} {}", request.getCurrency(), request.getTotalAmount());

        if (request.getStudentName() == null || request.getStudentName().isBlank()) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (request.getStudentEmail() == null || request.getStudentEmail().isBlank()) {
            throw new IllegalArgumentException("Student email is required");
        }
        if (request.getSessions() == null || request.getSessions().isEmpty()) {
            throw new IllegalArgumentException("At least one session is required");
        }

        List<Booking.SessionTime> sessionTimes = new ArrayList<>();
        for (BatchBookingRequest.SessionSlot slot : request.getSessions()) {
            if (slot.getSessionStartTime() == null || slot.getSessionEndTime() == null) {
                throw new IllegalArgumentException("Session start and end times are required");
            }
            if (slot.getSessionStartTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Cannot book sessions in the past");
            }

            // ✅ SAME-DAY LOGIC

            if (slot.getSessionEndTime().isBefore(slot.getSessionStartTime())) {
                throw new IllegalArgumentException("Session end time must be after start time");
            }

            sessionTimes.add(Booking.SessionTime.builder()
                    .startTime(slot.getSessionStartTime())
                    .endTime(slot.getSessionEndTime())
                    .amount(slot.getAmount())
                    .build());
        }

        Booking booking = Booking.builder()
                .studentId(studentId)
                .studentName(request.getStudentName())
                .studentEmail(request.getStudentEmail())
                .teacherId(request.getTeacherId())
                .courseId(request.getCourseId())
                .sessions(sessionTimes)
                .amount(request.getTotalAmount())
                .currency(request.getCurrency())
                .status(BookingStatus.PENDING)
                .bookedAt(LocalDateTime.now())
                .cancellationPolicy(getDefaultCancellationPolicy())
                .reminderSent(false)
                .notes(request.getNotes())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isFreeDemo(false)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("✅ Multi-session booking created: ID={}, Sessions={}, Total={}{}",
                savedBooking.getId(),
                sessionTimes.size(),
                request.getCurrency(),
                request.getTotalAmount());

        eventPublisher.publishBookingCreated(savedBooking);

        return toDto(savedBooking);
    }

    // ==================== CONFIRM BOOKING (AFTER PAYMENT) ====================

    @Transactional
    public BookingDto confirmBooking(String bookingId, String paymentId, String transactionId) {
        log.info("💳 Confirming booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (Boolean.TRUE.equals(booking.getIsFreeDemo())) {
            throw new IllegalArgumentException("Free demo booking is already confirmed; no payment required");
        }

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
        log.info("✅ Booking confirmed: {}", bookingId);

        eventPublisher.publishBookingConfirmed(updated);
        return toDto(updated);
    }

    // ==================== TEACHER APPROVE/REJECT ====================

    @Transactional
    public BookingDto approveBooking(String bookingId, String teacherId, String teacherMessage) {
        log.info("👍 Teacher {} approving booking {}", teacherId, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be approved");
        }

        if (Boolean.TRUE.equals(booking.getIsFreeDemo())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(LocalDateTime.now());
            log.info("✅ Free demo booking auto-confirmed after teacher approval: {}", bookingId);
        } else {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
        }

        if (teacherMessage != null && !teacherMessage.isEmpty()) {
            String existingNotes = booking.getNotes() != null ? booking.getNotes() : "";
            booking.setNotes(existingNotes + (existingNotes.isEmpty() ? "" : "\n\n") +
                    "Teacher's message: " + teacherMessage);
        }

        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking approved: {} - Student: {}", bookingId, booking.getStudentName());

        eventPublisher.publishBookingApproved(updated);

        return toDto(updated);
    }

    @Transactional
    public BookingDto rejectBooking(String bookingId, String teacherId, String rejectionReason) {
        log.info("👎 Teacher {} rejecting booking {}", teacherId, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!booking.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be rejected");
        }

        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancellationReason(rejectionReason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(teacherId);
        booking.setUpdatedAt(LocalDateTime.now());

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking rejected: {} - Reason: {}", bookingId, rejectionReason);

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

        String timezoneId = getTeacherTimezone(teacherId); // ✅ NEW

        return bookings.stream()
                .map(b -> applyDisplayTimezone(b, toDto(b), timezoneId)) // ✅ NEW
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

        String timezoneId = getTeacherTimezone(teacherId); // ✅ NEW

        return pending.stream()
                .map(b -> applyDisplayTimezone(b, toDto(b), timezoneId)) // ✅ NEW
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
                .teacherName(booking.getTeacherName())
                .parentId(booking.getParentId())
                .subject(booking.getSubject())
                .durationMinutes(booking.getDurationMinutes())
                .status(booking.getStatus())
                .sessionStartTime(booking.getSessionStartTime())
                .sessionEndTime(booking.getSessionEndTime())
                .sessions(sessionDtos)
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
                .isFreeDemo(booking.getIsFreeDemo())
                .build();
    }

    // ─────────────────────────────────────────────────────
    // PRIVATE: Auto-create video session after booking is confirmed
    // ─────────────────────────────────────────────────────
    private void createVideoSessionSafe(Booking booking) {
        log.info("🎥 [BookingService] Auto-creating video session for bookingId={}",
                booking.getId());
        try {
            String rawId = booking.getId().replace("-", "");
            String channelName = "session_" +
                    rawId.substring(0, Math.min(12, rawId.length()));

            LocalDateTime endTime = booking.getSessionEndTime();
            if (endTime == null
                    && booking.getSessionStartTime() != null
                    && booking.getDurationMinutes() != null) {
                endTime = booking.getSessionStartTime()
                        .plusMinutes(booking.getDurationMinutes());
            }

            VideoSessionCreateRequest videoRequest =
                    VideoSessionCreateRequest.builder()
                            .bookingId(booking.getId())
                            .classSessionId(booking.getSessionId())
                            .teacherId(booking.getTeacherId())
                            .studentId(booking.getStudentId())
                            .subject(booking.getSubject())
                            .scheduledStartTime(booking.getSessionStartTime())
                            .scheduledEndTime(endTime)
                            .channelName(channelName)
                            .recordingEnabled(true)
                            .build();

            VideoSessionCreateResponse response =
                    videoServiceClient.createVideoSession(videoRequest);

            if (response != null) {
                log.info("✅ [BookingService] Video session created: id={}, channel={}",
                        response.getId(), response.getChannelName());
            } else {
                log.warn("⚠️ [BookingService] Video session creation returned null " +
                        "(video-service may be down). bookingId={}", booking.getId());
            }

        } catch (Exception e) {
            log.error("❌ [BookingService] Failed to create video session for " +
                    "bookingId={}: {}", booking.getId(), e.getMessage());
        }
    }

    private void validateBookingTime(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now();

        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException("Cannot book in the past");
        }

        if (startTime.toLocalDate().equals(now.toLocalDate())) {
            if (startTime.isBefore(now.plusMinutes(30))) {
                throw new IllegalArgumentException(
                        "Same-day booking requires at least 30 minutes advance"
                );
            }
        }
    }

    // ─────────────────────────────────────────────
    // ✅ Timezone helpers for teacher display
    // ─────────────────────────────────────────────

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("MMM d, hh:mm a");

    private String getTeacherTimezone(String teacherId) {
        return teacherAvailabilityRepository.findByTeacherId(teacherId)
                .map(TeacherAvailability::getTimezone)
                .filter(tz -> tz != null && !tz.isBlank())
                .orElse("Asia/Kolkata");
    }

    private BookingDto applyDisplayTimezone(Booking booking, BookingDto dto, String timezoneId) {
        if (timezoneId == null || timezoneId.isBlank()) {
            timezoneId = "Asia/Kolkata";
        }

        ZoneId targetZone = ZoneId.of(timezoneId);

        if (booking.getSessionStartTime() != null) {
            ZonedDateTime z = booking.getSessionStartTime()
                    .atZone(UTC)
                    .withZoneSameInstant(targetZone);
            dto.setDisplaySessionStartTime(z.format(DISPLAY_FMT));
        }

        if (booking.getSessionEndTime() != null) {
            ZonedDateTime z = booking.getSessionEndTime()
                    .atZone(UTC)
                    .withZoneSameInstant(targetZone);
            dto.setDisplaySessionEndTime(z.format(DISPLAY_FMT));
        }

        if (booking.getBookedAt() != null) {
            ZonedDateTime z = booking.getBookedAt()
                    .atZone(UTC)
                    .withZoneSameInstant(targetZone);
            dto.setDisplayBookedAt(z.format(DISPLAY_FMT));
        }

        dto.setDisplayTimezoneId(timezoneId);
        dto.setDisplayTimezoneAbbreviation(
                ZonedDateTime.now(targetZone).format(DateTimeFormatter.ofPattern("z"))
        );

        return dto;
    }
}