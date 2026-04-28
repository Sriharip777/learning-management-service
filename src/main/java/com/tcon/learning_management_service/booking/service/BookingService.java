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
import com.tcon.learning_management_service.session.entity.SessionType;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, hh:mm a");
    private static final int MIN_BOOKING_LEAD_MINUTES = 30;
    private static final int MIN_SESSION_DURATION_MINUTES = 30;
    private static final int MAX_SESSION_DURATION_MINUTES = 180;

    @Transactional
    public BookingDto createBooking(String studentId, BookingRequest request) {
        log.info("📥 Creating booking for student: {}", studentId);
        log.info("📋 Request: {}", request);
        log.info("👤 Student info - Name: {}, Email: {}", request.getStudentName(), request.getStudentEmail());

        validateStudentDetails(request);

        if (hasText(request.getSessionId())) {
            return createBookingForExistingSession(studentId, request);
        } else if (hasText(request.getTeacherId())) {
            return createDirectTeacherBooking(studentId, request);
        } else {
            throw new IllegalArgumentException("Either sessionId or teacherId must be provided");
        }
    }

    private BookingDto createBookingForExistingSession(String studentId, BookingRequest request) {
        log.info("📋 Creating booking for existing session: {}", request.getSessionId());

        ClassSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        if (session.getStatus() != ClassStatus.SCHEDULED) {
            throw new IllegalArgumentException("Session is not available for booking");
        }

        validateBookingTime(session.getScheduledStartTime());

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

            boolean isFreeDemo = tryConsumeFreeDemo(studentId, request.getSessionId());

            LocalDateTime now = LocalDateTime.now(APP_ZONE);

            Booking booking = Booking.builder()
                    .sessionId(request.getSessionId())
                    .courseId(session.getCourseId())
                    .studentId(studentId)
                    .studentName(request.getStudentName())
                    .studentEmail(request.getStudentEmail())
                    .teacherId(session.getTeacherId())
                    .teacherName(session.getTeacherName())
                    .parentId(request.getParentId())
                    .subject(request.getSubject())
                    .durationMinutes(resolveDurationMinutes(session.getScheduledStartTime(), session.getScheduledEndTime()))
                    .status(BookingStatus.PENDING)
                    .sessionStartTime(session.getScheduledStartTime())
                    .sessionEndTime(session.getScheduledEndTime())
                    .amount(isFreeDemo ? BigDecimal.ZERO : defaultAmount(request.getAmount()))
                    .currency(defaultCurrency(request.getCurrency()))
                    .bookedAt(now)
                    .cancellationPolicy(getDefaultCancellationPolicy())
                    .reminderSent(false)
                    .notes(request.getNotes())
                    .createdAt(now)
                    .updatedAt(now)
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

    private BookingDto createDirectTeacherBooking(String studentId, BookingRequest request) {
        log.info("🎯 Creating direct one-on-one booking for teacher: {}", request.getTeacherId());
        log.info("📅 Time: {} to {}", request.getSessionStartTime(), request.getSessionEndTime());

        validateDirectBookingRequest(request);

        ZoneId userZone = ZoneId.of("Asia/Kolkata");

        LocalDateTime startUtc = request.getSessionStartTime()
                .atZone(userZone)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime();

        LocalDateTime endUtc = request.getSessionEndTime()
                .atZone(userZone)
                .withZoneSameInstant(ZoneId.of("UTC"))
                .toLocalDateTime();

        String lockKey = buildTeacherSlotLockKey(
                request.getTeacherId(),
                startUtc,
                endUtc
        );

        if (!lockService.acquireLock(lockKey, studentId)) {
            throw new IllegalArgumentException("Selected time slot is currently being booked by another user");
        }

        try {
            ensureNoTeacherOverlap(
                    request.getTeacherId(),
                    startUtc,
                    endUtc
            );

            int duration = resolveDurationMinutes(
                    startUtc,
                    endUtc
            );

            log.info("📏 Calculated duration: {} minutes", duration);

            ClassSession session = ClassSession.builder()
                    .sessionType(SessionType.ONE_ON_ONE)
                    .courseId(null)
                    .teacherId(request.getTeacherId())
                    .teacherName(request.getTeacherName() != null ? request.getTeacherName() : "Teacher")
                    .studentId(studentId)
                    .bookingId(null)
                    .title(hasText(request.getSubject()) ? request.getSubject() : "One-on-One Class")
                    .description("Direct booking with " + request.getStudentName())
                    .status(ClassStatus.SCHEDULED)
                    .scheduledStartTime(startUtc)
                    .scheduledEndTime(endUtc)
                    .durationMinutes(duration)
                    .maxParticipants(1)
                    .participants(new ArrayList<>())
                    .attendedCount(0)
                    .materialUrls(new ArrayList<>())
                    .reminderSent(false)
                    .createdBy(request.getTeacherId())
                    .build();

            ClassSession savedSession = sessionRepository.save(session);
            log.info("✅ ClassSession created: {} (Type: ONE_ON_ONE)", savedSession.getId());

            boolean isFreeDemo = tryConsumeFreeDemo(studentId, savedSession.getId());
            LocalDateTime now = LocalDateTime.now(APP_ZONE);

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
                    .sessionStartTime(startUtc)
                    .sessionEndTime(endUtc)
                    .status(BookingStatus.PENDING)
                    .amount(isFreeDemo ? BigDecimal.ZERO : defaultAmount(request.getAmount()))
                    .currency(defaultCurrency(request.getCurrency()))
                    .bookedAt(now)
                    .cancellationPolicy(getDefaultCancellationPolicy())
                    .reminderSent(false)
                    .notes(request.getNotes())
                    .createdAt(now)
                    .updatedAt(now)
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

        } finally {
            lockService.releaseLock(lockKey, studentId);
        }
    }

    @Transactional
    public BookingDto createBatchBooking(String studentId, BatchBookingRequest request) {
        log.info("📦 Creating multi-session booking for student: {}", studentId);
        log.info("  - Student: {} ({})", request.getStudentName(), request.getStudentEmail());
        log.info("  - Teacher: {}", request.getTeacherId());
        log.info("  - Sessions: {}", request.getSessions().size());
        log.info("  - Total amount: {} {}", request.getCurrency(), request.getTotalAmount());

        if (!hasText(request.getStudentName())) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (!hasText(request.getStudentEmail())) {
            throw new IllegalArgumentException("Student email is required");
        }
        if (request.getSessions() == null || request.getSessions().isEmpty()) {
            throw new IllegalArgumentException("At least one session is required");
        }

        List<Booking.SessionTime> sessionTimes = new ArrayList<>();
        for (BatchBookingRequest.SessionSlot slot : request.getSessions()) {
            validateSessionRange(slot.getSessionStartTime(), slot.getSessionEndTime());
            validateBookingTime(slot.getSessionStartTime());

            sessionTimes.add(Booking.SessionTime.builder()
                    .startTime(slot.getSessionStartTime())
                    .endTime(slot.getSessionEndTime())
                    .amount(slot.getAmount())
                    .build());
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        Booking booking = Booking.builder()
                .studentId(studentId)
                .studentName(request.getStudentName())
                .studentEmail(request.getStudentEmail())
                .teacherId(request.getTeacherId())
                .courseId(request.getCourseId())
                .sessions(sessionTimes)
                .amount(request.getTotalAmount())
                .currency(defaultCurrency(request.getCurrency()))
                .status(BookingStatus.PENDING)
                .bookedAt(now)
                .cancellationPolicy(getDefaultCancellationPolicy())
                .reminderSent(false)
                .notes(request.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .isFreeDemo(false)
                .build();

        Booking savedBooking = bookingRepository.save(booking);
        log.info("✅ Multi-session booking created: ID={}, Sessions={}, Total={}{}",
                savedBooking.getId(), sessionTimes.size(), request.getCurrency(), request.getTotalAmount());

        eventPublisher.publishBookingCreated(savedBooking);
        return toDto(savedBooking);
    }

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

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setPaymentId(paymentId);
        booking.setTransactionId(transactionId);
        booking.setConfirmedAt(now);
        booking.setUpdatedAt(now);

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking confirmed: {}", bookingId);

        createVideoSessionSafe(updated);
        eventPublisher.publishBookingConfirmed(updated);
        return toDto(updated);
    }

    @Transactional
    public BookingDto approveBooking(String bookingId, String teacherId, String teacherMessage) {
        log.info("👍 Teacher {} approving booking {}", teacherId, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("❌ Booking not found for approve: {}", bookingId);
                    return new IllegalArgumentException("Booking not found: " + bookingId);
                });

        if (!Objects.equals(booking.getTeacherId(), teacherId)) {
            log.warn("❌ Unauthorized approve attempt. bookingId={}, bookingTeacherId={}, headerTeacherId={}",
                    bookingId, booking.getTeacherId(), teacherId);
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            log.warn("❌ Invalid status for approve. bookingId={}, status={}", bookingId, booking.getStatus());
            throw new IllegalArgumentException("Only pending bookings can be approved");
        }

        if (!hasText(booking.getSessionId())) {
            log.error("❌ Cannot approve booking without sessionId. bookingId={}", bookingId);
            throw new IllegalStateException("Cannot approve booking without linked session");
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        boolean confirmedImmediately = false;

        if (Boolean.TRUE.equals(booking.getIsFreeDemo())) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(now);
            confirmedImmediately = true;
            log.info("✅ Free demo booking auto-confirmed after teacher approval: {}", bookingId);
        } else {
            booking.setStatus(BookingStatus.PENDING_PAYMENT);
            log.info("✅ Booking moved to PENDING_PAYMENT after teacher approval: {}", bookingId);
        }

        if (hasText(teacherMessage)) {
            String existingNotes = booking.getNotes() != null ? booking.getNotes() : "";
            booking.setNotes(existingNotes + (existingNotes.isEmpty() ? "" : "\n\n") +
                    "Teacher's message: " + teacherMessage);
        }

        booking.setUpdatedAt(now);

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking approved: {} - Student: {}", bookingId, booking.getStudentName());

        if (confirmedImmediately) {
            createVideoSessionSafe(updated);
        }

        try {
            eventPublisher.publishBookingApproved(updated);
            log.info("📤 BOOKING_APPROVED event published for booking {}", bookingId);
        } catch (Exception e) {
            log.error("❌ Failed to publish BOOKING_APPROVED event for booking {}: {}",
                    bookingId, e.getMessage(), e);
        }

        return toDto(updated);
    }

    @Transactional
    public BookingDto rejectBooking(String bookingId, String teacherId, String rejectionReason) {
        log.info("👎 Teacher {} rejecting booking {}", teacherId, bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (!Objects.equals(booking.getTeacherId(), teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only pending bookings can be rejected");
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setCancellationReason(rejectionReason);
        booking.setCancelledAt(now);
        booking.setCancelledBy(teacherId);
        booking.setUpdatedAt(now);

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking rejected: {} - Reason: {}", bookingId, rejectionReason);

        eventPublisher.publishBookingRejected(updated);
        return toDto(updated);
    }

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

        return bookings.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookingDto> getTeacherBookings(String teacherId) {
        log.info("📋 Getting bookings for teacher: {}", teacherId);

        List<Booking> bookings = bookingRepository.findByTeacherId(teacherId);
        log.info("✅ Found {} bookings for teacher", bookings.size());

        String timezoneId = getTeacherTimezone(teacherId);

        return bookings.stream()
                .map(b -> applyDisplayTimezone(b, toDto(b), timezoneId))
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

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        return bookingRepository.findByStudentIdAndSessionStartTimeBetween(studentId, now, now.plusMonths(1))
                .stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getParentBookings(String parentId) {
        log.info("📋 Getting bookings for parent: {}", parentId);

        List<Booking> bookings = bookingRepository.findByParentId(parentId);
        log.info("✅ Found {} bookings for parent {}", bookings.size(), parentId);

        return bookings.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<BookingDto> getTeacherPendingRequests(String teacherId) {
        log.info("📋 Getting pending requests for teacher: {}", teacherId);

        List<Booking> pending = bookingRepository.findByTeacherIdAndStatus(teacherId, BookingStatus.PENDING);
        log.info("✅ Found {} pending requests for teacher", pending.size());

        pending.forEach(booking -> {
            int sessionCount = (booking.getSessions() != null && !booking.getSessions().isEmpty())
                    ? booking.getSessions().size() : 1;
            log.info("  📌 Pending: ID={}, Student={}, Email={}, Sessions={}, Amount={}",
                    booking.getId(),
                    booking.getStudentName(),
                    booking.getStudentEmail(),
                    sessionCount,
                    booking.getAmount());
        });

        String timezoneId = getTeacherTimezone(teacherId);

        return pending.stream()
                .map(b -> applyDisplayTimezone(b, toDto(b), timezoneId))
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingDto completeBooking(String bookingId) {
        log.info("✅ Completing booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Only confirmed bookings can be completed");
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        booking.setStatus(BookingStatus.COMPLETED);
        booking.setCompletedAt(now);
        booking.setUpdatedAt(now);

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking completed: {}", bookingId);

        return toDto(updated);
    }

    @Transactional
    public BookingDto cancelBooking(String bookingId, String userId, String reason) {
        log.info("❌ Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        boolean isStudent = Objects.equals(booking.getStudentId(), userId);
        boolean isTeacher = Objects.equals(booking.getTeacherId(), userId);

        if (!isStudent && !isTeacher) {
            throw new IllegalArgumentException("Unauthorized: Only student or teacher can cancel");
        }

        if (booking.getStatus() != BookingStatus.PENDING &&
                booking.getStatus() != BookingStatus.PENDING_PAYMENT &&
                booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot cancel booking in current status");
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(now);
        booking.setCancelledBy(userId);
        booking.setUpdatedAt(now);

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking cancelled: {}", bookingId);

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

    private void createVideoSessionSafe(Booking booking) {
        try {
            if (booking == null) {
                log.warn("Cannot create video session: booking is null");
                return;
            }

            if (!hasText(booking.getId())) {
                log.warn("Cannot create video session: booking id is missing");
                return;
            }

            if (!hasText(booking.getSessionId())) {
                log.warn("Cannot create video session: classSessionId is missing for booking {}", booking.getId());
                return;
            }

            if (!hasText(booking.getTeacherId())) {
                log.warn("Cannot create video session: teacher id is missing for booking {}", booking.getId());
                return;
            }

            if (!hasText(booking.getStudentId())) {
                log.warn("Cannot create video session: student id is missing for booking {}", booking.getId());
                return;
            }

            if (booking.getSessionStartTime() == null) {
                log.warn("Cannot create video session: session start time is missing for booking {}", booking.getId());
                return;
            }

            if (booking.getSessionEndTime() == null) {
                log.warn("Cannot create video session: session end time is missing for booking {}", booking.getId());
                return;
            }

            if (booking.getDurationMinutes() == null || booking.getDurationMinutes() <= 0) {
                log.warn("Cannot create video session: durationMinutes is missing/invalid for booking {}", booking.getId());
                return;
            }

            VideoSessionCreateRequest videoRequest = VideoSessionCreateRequest.builder()
                    .bookingId(booking.getId())
                    .classSessionId(booking.getSessionId())
                    .teacherId(booking.getTeacherId())
                    .studentId(booking.getStudentId())
                    .parentId(booking.getParentId())
                    .subject(hasText(booking.getSubject()) ? booking.getSubject() : "One-on-One Class")
                    .scheduledStartTime(booking.getSessionStartTime())
                    .scheduledEndTime(booking.getSessionEndTime())
                    .durationMinutes(booking.getDurationMinutes())
                    .channelName("booking-" + booking.getId())
                    .recordingEnabled(true)
                    .whiteboardEnabled(true)
                    .chatEnabled(true)
                    .build();

            log.info("📹 Creating video session for booking: {}, classSessionId: {}, teacherId: {}, studentId: {}, startTime: {}, endTime: {}, durationMinutes: {}",
                    booking.getId(),
                    booking.getSessionId(),
                    booking.getTeacherId(),
                    booking.getStudentId(),
                    booking.getSessionStartTime(),
                    booking.getSessionEndTime(),
                    booking.getDurationMinutes());

            VideoSessionCreateResponse response = videoServiceClient.createVideoSession(videoRequest);

            if (response == null) {
                log.error("❌ Video service returned null response for booking {}", booking.getId());
                return;
            }

            log.info("✅ Video session created successfully. bookingId={}, videoSessionId={}, channelName={}, canJoin={}",
                    booking.getId(),
                    response.getId(),
                    response.getChannelName(),
                    response.getCanJoin());

        } catch (Exception e) {
            log.error("❌ Failed to create video session for booking {}: {}",
                    booking != null ? booking.getId() : null, e.getMessage(), e);
        }
    }

    private void validateBookingTime(LocalDateTime startTime) {
        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        if (startTime == null) {
            throw new IllegalArgumentException("Session start time is required");
        }

        if (startTime.isBefore(now)) {
            throw new IllegalArgumentException("Cannot book in the past");
        }

        if (startTime.toLocalDate().equals(now.toLocalDate()) &&
                startTime.isBefore(now.plusMinutes(MIN_BOOKING_LEAD_MINUTES))) {
            throw new IllegalArgumentException(
                    "Same-day booking requires at least " + MIN_BOOKING_LEAD_MINUTES + " minutes advance"
            );
        }
    }

    private void validateDirectBookingRequest(BookingRequest request) {
        validateStudentDetails(request);

        if (!hasText(request.getTeacherId())) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        validateSessionRange(request.getSessionStartTime(), request.getSessionEndTime());
        validateBookingTime(request.getSessionStartTime());
    }

    private void validateSessionRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Session start and end times are required");
        }

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("Session end time must be after start time");
        }

        int durationMinutes = (int) Duration.between(startTime, endTime).toMinutes();

        if (durationMinutes < MIN_SESSION_DURATION_MINUTES) {
            throw new IllegalArgumentException("Session duration must be at least " + MIN_SESSION_DURATION_MINUTES + " minutes");
        }

        if (durationMinutes > MAX_SESSION_DURATION_MINUTES) {
            throw new IllegalArgumentException("Session duration cannot exceed " + MAX_SESSION_DURATION_MINUTES + " minutes");
        }
    }

    private void validateStudentDetails(BookingRequest request) {
        if (!hasText(request.getStudentName())) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (!hasText(request.getStudentEmail())) {
            throw new IllegalArgumentException("Student email is required");
        }
    }

    private int resolveDurationMinutes(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return 0;
        }
        return (int) Duration.between(startTime, endTime).toMinutes();
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private String defaultCurrency(String currency) {
        return hasText(currency) ? currency : "INR";
    }

    private boolean tryConsumeFreeDemo(String studentId, String referenceId) {
        try {
            demoLimitService.consumeFreeDemo(studentId);
            log.info("Using free demo for student {} on reference {}", studentId, referenceId);
            return true;
        } catch (Exception ex) {
            log.info("No free demo available for student {}: {}", studentId, ex.getMessage());
            return false;
        }
    }

    private void ensureNoTeacherOverlap(String teacherId,
                                        LocalDateTime requestedStart,
                                        LocalDateTime requestedEnd) {

        List<ClassSession> overlappingSessions =
                sessionRepository.findOverlappingSessions(
                        teacherId,
                        requestedStart,
                        requestedEnd
                );

        if (!overlappingSessions.isEmpty()) {
            throw new IllegalArgumentException("Time slot already booked for this teacher");
        }
    }

    private String buildTeacherSlotLockKey(String teacherId, LocalDateTime startTime, LocalDateTime endTime) {
        return "teacher:" + teacherId + ":" + startTime + ":" + endTime;
    }

    private String getTeacherTimezone(String teacherId) {
        return teacherAvailabilityRepository.findByTeacherId(teacherId)
                .map(TeacherAvailability::getTimezone)
                .filter(this::hasText)
                .orElse(APP_ZONE.getId());
    }

    private BookingDto applyDisplayTimezone(Booking booking, BookingDto dto, String timezoneId) {
        String safeTimezoneId = hasText(timezoneId) ? timezoneId : APP_ZONE.getId();
        ZoneId teacherZone = ZoneId.of(safeTimezoneId);

        if (booking.getSessionStartTime() != null) {
            ZonedDateTime z = booking.getSessionStartTime().atZone(teacherZone);
            dto.setDisplaySessionStartTime(z.format(DISPLAY_FMT));
        }

        if (booking.getSessionEndTime() != null) {
            ZonedDateTime z = booking.getSessionEndTime().atZone(teacherZone);
            dto.setDisplaySessionEndTime(z.format(DISPLAY_FMT));
        }

        if (booking.getBookedAt() != null) {
            ZonedDateTime z = booking.getBookedAt().atZone(teacherZone);
            dto.setDisplayBookedAt(z.format(DISPLAY_FMT));
        }

        dto.setDisplayTimezoneId(safeTimezoneId);
        dto.setDisplayTimezoneAbbreviation(
                ZonedDateTime.now(teacherZone).format(DateTimeFormatter.ofPattern("z"))
        );

        return dto;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}