package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import com.tcon.learning_management_service.booking.dto.BatchBookingRequest;
import com.tcon.learning_management_service.booking.dto.BookingDto;
import com.tcon.learning_management_service.booking.dto.BookingRequest;
import com.tcon.learning_management_service.booking.dto.TeacherAssignStudentsBookingRequest;
import com.tcon.learning_management_service.booking.dto.TeacherAssignedStudentDto;
import com.tcon.learning_management_service.booking.dto.TeacherGradeSubjectDto;
import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.entity.CancellationPolicy;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.client.CourseClient;
import com.tcon.learning_management_service.client.UserServiceClient;
import com.tcon.learning_management_service.client.VideoServiceClient;
import com.tcon.learning_management_service.client.dto.AssignedStudentOptionDto;
import com.tcon.learning_management_service.client.dto.CourseDto;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateRequest;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateResponse;
import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.service.CourseService;
import com.tcon.learning_management_service.course.service.GradeService;
import com.tcon.learning_management_service.course.service.SubjectService;
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
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final UserServiceClient userServiceClient;
    private final BookingRepository bookingRepository;
    private final ClassSessionRepository sessionRepository;
    private final BookingEventPublisher eventPublisher;
    private final BookingLockService lockService;
    private final DemoLimitService demoLimitService;
    private final VideoServiceClient videoServiceClient;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    private final CourseClient courseClient;
    private final CourseService courseService;
    private final GradeService gradeService;
    private final SubjectService subjectService;

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("MMM d, hh:mm a");
    private static final int MIN_BOOKING_LEAD_MINUTES = 30;
    private static final String DEFAULT_CURRENCY = "INR";
    private static final String DEFAULT_TEACHER_NAME = "Teacher";
    private static final String DEFAULT_STUDENT_NAME = "Student";

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

    private String resolveSubjectName(String courseId) {
        try {
            if (courseId == null) {
                return null;
            }

            CourseDto course = courseClient.getCourseById(courseId);
            if (course != null) {
                return course.getSubject();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch subject for courseId={}", courseId);
        }

        return null;
    }

    private int getFreeSlotsToApply(String studentId, int requestedSlots) {
        int remaining = demoLimitService.getRemainingFreeDemos(studentId);
        return Math.min(remaining, requestedSlots);
    }

    private BigDecimal resolvePaidSlotAmount(BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Paid slot amount must be greater than 0");
        }
        return requestedAmount;
    }

    private BookingDto createBookingForExistingSession(String studentId, BookingRequest request) {
        log.info("📋 Creating booking for existing session: {}", request.getSessionId());

        ClassSession session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + request.getSessionId()));

        String subjectName = resolveSubjectName(session.getCourseId());

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

            int freeSlotsApplied = getFreeSlotsToApply(studentId, 1);
            boolean isFreeDemo = freeSlotsApplied == 1;

            BigDecimal finalAmount = isFreeDemo
                    ? BigDecimal.ZERO
                    : resolvePaidSlotAmount(defaultAmount(request.getAmount()));

            LocalDateTime now = LocalDateTime.now(APP_ZONE);

            Booking booking = Booking.builder()
                    .sessionId(request.getSessionId())
                    .courseId(session.getCourseId())
                    .studentId(studentId)
                    .subjectName(subjectName)
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
                    .amount(finalAmount)
                    .currency(defaultCurrency(request.getCurrency()))
                    .bookedAt(now)
                    .cancellationPolicy(getDefaultCancellationPolicy())
                    .reminderSent(false)
                    .notes(request.getNotes())
                    .createdAt(now)
                    .updatedAt(now)
                    .isFreeDemo(isFreeDemo)
                    .freeSlotsApplied(freeSlotsApplied)
                    .paidSlotsApplied(isFreeDemo ? 0 : 1)
                    .build();

            Booking saved = bookingRepository.save(booking);

            if (freeSlotsApplied > 0) {
                demoLimitService.consumeFreeDemos(studentId, freeSlotsApplied);
            }

            log.info("✅ Booking created: ID={}, Student={}, Session={}, isFreeDemo={}, freeSlotsApplied={}, paidSlotsApplied={}, amount={}",
                    saved.getId(), saved.getStudentName(), saved.getSessionId(), saved.getIsFreeDemo(),
                    saved.getFreeSlotsApplied(), saved.getPaidSlotsApplied(), saved.getAmount());

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
            ensureNoTeacherOverlap(request.getTeacherId(), startUtc, endUtc);

            int duration = resolveDurationMinutes(startUtc, endUtc);
            log.info("📏 Calculated duration: {} minutes", duration);

            ClassSession session = ClassSession.builder()
                    .sessionType(SessionType.ONE_ON_ONE)
                    .courseId(null)
                    .teacherId(request.getTeacherId())
                    .teacherName(request.getTeacherName() != null ? request.getTeacherName() : DEFAULT_TEACHER_NAME)
                    .studentId(studentId)
                    .bookingId(null)
                    .title(hasText(request.getSubject()) ? request.getSubject() : "One-on-One Class")
                    .description("Direct booking with " + request.getStudentName())
                    .status(ClassStatus.SCHEDULED)
                    .scheduledStartTime(request.getSessionStartTime())
                    .scheduledEndTime(request.getSessionEndTime())
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

            int freeSlotsApplied = getFreeSlotsToApply(studentId, 1);
            boolean isFreeDemo = freeSlotsApplied == 1;

            BigDecimal finalAmount = isFreeDemo
                    ? BigDecimal.ZERO
                    : resolvePaidSlotAmount(defaultAmount(request.getAmount()));

            LocalDateTime now = LocalDateTime.now(APP_ZONE);

            Booking booking = Booking.builder()
                    .sessionId(savedSession.getId())
                    .courseId(null)
                    .studentId(studentId)
                    .subjectName(request.getSubject())
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
                    .amount(finalAmount)
                    .currency(defaultCurrency(request.getCurrency()))
                    .bookedAt(now)
                    .cancellationPolicy(getDefaultCancellationPolicy())
                    .reminderSent(false)
                    .notes(request.getNotes())
                    .createdAt(now)
                    .updatedAt(now)
                    .isFreeDemo(isFreeDemo)
                    .freeSlotsApplied(freeSlotsApplied)
                    .paidSlotsApplied(isFreeDemo ? 0 : 1)
                    .build();

            Booking saved = bookingRepository.save(booking);

            if (freeSlotsApplied > 0) {
                demoLimitService.consumeFreeDemos(studentId, freeSlotsApplied);
            }

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
            log.info("   🆓 freeSlotsApplied: {}", saved.getFreeSlotsApplied());
            log.info("   💰 paidSlotsApplied: {}", saved.getPaidSlotsApplied());
            log.info("   💵 amount: {}", saved.getAmount());

            eventPublisher.publishBookingCreated(saved);
            return toDto(saved);

        } finally {
            lockService.releaseLock(lockKey, studentId);
        }
    }

    public List<TeacherGradeSubjectDto> getTeacherGradeSubjects(String teacherId) {
        log.info("🔥 API HIT: getTeacherGradeSubjects for teacherId={}", teacherId);

        List<Course> courses = courseService.getCoursesByTeacherId(teacherId);
        Map<String, Set<String>> map = new HashMap<>();

        for (Course course : courses) {
            if (course.getGradeId() == null || course.getSubjectId() == null) {
                continue;
            }

            String gradeName = gradeService.getAll().stream()
                    .filter(g -> g.getId().equals(course.getGradeId()))
                    .map(g -> g.getName())
                    .findFirst()
                    .orElse(null);

            String subjectName = subjectService.getByGrade(course.getGradeId()).stream()
                    .filter(s -> s.getId().equals(course.getSubjectId()))
                    .map(s -> s.getName())
                    .findFirst()
                    .orElse(null);

            if (gradeName == null || subjectName == null) {
                continue;
            }

            map.computeIfAbsent(gradeName, k -> new HashSet<>())
                    .add(subjectName);
        }

        return map.entrySet().stream()
                .map(entry -> TeacherGradeSubjectDto.builder()
                        .grade(entry.getKey())
                        .subjects(new ArrayList<>(entry.getValue()))
                        .build())
                .toList();
    }

    @Transactional
    public BookingDto createBatchBooking(String studentId, BatchBookingRequest request) {
        log.info("📦 Creating multi-session booking for student: {}", studentId);
        log.info("  - Student: {} ({})", request.getStudentName(), request.getStudentEmail());
        log.info("  - Teacher: {}", request.getTeacherId());
        log.info("  - Sessions: {}", request.getSessions().size());
        log.info("  - Requested total amount: {} {}", request.getCurrency(), request.getTotalAmount());

        if (!hasText(request.getStudentName())) {
            throw new IllegalArgumentException("Student name is required");
        }
        if (!hasText(request.getStudentEmail())) {
            throw new IllegalArgumentException("Student email is required");
        }
        if (request.getSessions() == null || request.getSessions().isEmpty()) {
            throw new IllegalArgumentException("At least one session is required");
        }

        List<BatchBookingRequest.SessionSlot> sortedSlots = request.getSessions().stream()
                .sorted((a, b) -> a.getSessionStartTime().compareTo(b.getSessionStartTime()))
                .toList();

        int requestedSlots = sortedSlots.size();
        int freeSlotsApplied = getFreeSlotsToApply(studentId, requestedSlots);
        int paidSlotsApplied = requestedSlots - freeSlotsApplied;

        List<Booking.SessionTime> sessionTimes = new ArrayList<>();
        BigDecimal recalculatedTotal = BigDecimal.ZERO;

        for (int i = 0; i < sortedSlots.size(); i++) {
            BatchBookingRequest.SessionSlot slot = sortedSlots.get(i);

            validateSessionRange(slot.getSessionStartTime(), slot.getSessionEndTime());
            validateBookingTime(slot.getSessionStartTime());

            BigDecimal finalSlotAmount;
            if (i < freeSlotsApplied) {
                finalSlotAmount = BigDecimal.ZERO;
            } else {
                finalSlotAmount = resolvePaidSlotAmount(slot.getAmount());
            }

            recalculatedTotal = recalculatedTotal.add(finalSlotAmount);

            sessionTimes.add(Booking.SessionTime.builder()
                    .startTime(slot.getSessionStartTime())
                    .endTime(slot.getSessionEndTime())
                    .amount(finalSlotAmount)
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
                .amount(recalculatedTotal)
                .currency(defaultCurrency(request.getCurrency()))
                .subjectName(resolveSubjectName(request.getCourseId()))
                .status(BookingStatus.PENDING)
                .bookedAt(now)
                .cancellationPolicy(getDefaultCancellationPolicy())
                .reminderSent(false)
                .notes(request.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .isFreeDemo(paidSlotsApplied == 0)
                .freeSlotsApplied(freeSlotsApplied)
                .paidSlotsApplied(paidSlotsApplied)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        if (freeSlotsApplied > 0) {
            demoLimitService.consumeFreeDemos(studentId, freeSlotsApplied);
        }

        log.info("✅ Multi-session booking created: ID={}, Sessions={}, freeSlotsApplied={}, paidSlotsApplied={}, Total={} {}",
                savedBooking.getId(), sessionTimes.size(), freeSlotsApplied, paidSlotsApplied,
                savedBooking.getCurrency(), recalculatedTotal);

        eventPublisher.publishBookingCreated(savedBooking);
        return toDto(savedBooking);
    }

    @Transactional
    public BookingDto confirmBooking(String bookingId, String paymentId, String transactionId) {
        log.info("💳 Confirming booking: {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        if (booking.getAmount() == null || booking.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("This booking does not require payment");
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
            if (booking.getSessions() == null || booking.getSessions().isEmpty()) {
                log.error("❌ Cannot approve booking without sessionId or sessions. bookingId={}", bookingId);
                throw new IllegalStateException("Cannot approve booking without linked session or sessions");
            }
            log.warn("⚠️ Approving booking without single sessionId (multi-session booking). bookingId={}, sessions={}",
                    bookingId, booking.getSessions().size());
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        boolean confirmedImmediately = false;

        if (booking.getAmount() != null && booking.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setConfirmedAt(now);
            confirmedImmediately = true;
            log.info("✅ Fully free booking auto-confirmed after teacher approval: {}", bookingId);
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

        if (confirmedImmediately && hasText(updated.getSessionId())) {
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

        return toDtoWithVideoSession(booking);
    }

    public List<BookingDto> getStudentBookings(String studentId) {
        log.info("📋 Getting bookings for student: {}", studentId);

        List<Booking> bookings = bookingRepository.findByStudentId(studentId);
        log.info("✅ Found {} bookings for student", bookings.size());

        return bookings.stream()
                .map(this::toDtoWithVideoSession)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getTeacherBookings(String teacherId) {
        log.info("📋 Getting bookings for teacher: {}", teacherId);

        List<Booking> bookings = bookingRepository.findByTeacherId(teacherId);
        log.info("✅ Found {} bookings for teacher", bookings.size());

        String timezoneId = getTeacherTimezone(teacherId);

        return bookings.stream()
                .map(this::toDtoWithVideoSession)
                .map(dto -> applyDisplayTimezone(dto, timezoneId))
                .collect(Collectors.toList());
    }

    public List<BookingDto> getSessionBookings(String sessionId) {
        log.info("📋 Getting bookings for session: {}", sessionId);

        return bookingRepository.findBySessionId(sessionId).stream()
                .map(this::toDtoWithVideoSession)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getStudentUpcomingBookings(String studentId) {
        log.info("📋 Getting upcoming bookings for student: {}", studentId);

        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        return bookingRepository.findByStudentIdAndSessionStartTimeBetween(studentId, now, now.plusMonths(1))
                .stream()
                .map(this::toDtoWithVideoSession)
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .collect(Collectors.toList());
    }

    public List<BookingDto> getParentBookings(String parentId) {
        log.info("📋 Getting bookings for parent: {}", parentId);

        List<Booking> bookings = bookingRepository.findByParentId(parentId);
        log.info("✅ Found {} bookings for parent {}", bookings.size(), parentId);

        return bookings.stream()
                .map(this::toDtoWithVideoSession)
                .collect(Collectors.toList());
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
                .map(this::toDtoWithVideoSession)
                .map(dto -> applyDisplayTimezone(dto, timezoneId))
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
        booking.setCancellationReason(reasonOrDefault(reason));
        booking.setCancelledAt(now);
        booking.setCancelledBy(userId);
        booking.setUpdatedAt(now);

        Booking updated = bookingRepository.save(booking);
        log.info("✅ Booking cancelled: {}", bookingId);

        return toDto(updated);
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

    private void validateSessionRange(LocalDateTime start, LocalDateTime end) {
        if (start == null) {
            throw new IllegalArgumentException("Session start time is required");
        }
        if (end == null) {
            throw new IllegalArgumentException("Session end time is required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Session end time must be after session start time");
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

    private int resolveDurationMinutes(LocalDateTime start, LocalDateTime end) {
        return Math.toIntExact(Duration.between(start, end).toMinutes());
    }

    private BigDecimal defaultAmount(BigDecimal amount) {
        return amount != null ? amount : BigDecimal.ZERO;
    }

    private String defaultCurrency(String currency) {
        return hasText(currency) ? currency.trim() : DEFAULT_CURRENCY;
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

    private String buildTeacherSlotLockKey(String teacherId, LocalDateTime start, LocalDateTime end) {
        return "teacher-slot:" + teacherId + ":" + start + ":" + end;
    }

    private String getTeacherTimezone(String teacherId) {
        return teacherAvailabilityRepository.findByTeacherId(teacherId)
                .map(TeacherAvailability::getTimezone)
                .filter(this::hasText)
                .orElse(APP_ZONE.getId());
    }

    @Transactional(readOnly = true)
    public List<TeacherAssignedStudentDto> getAssignedStudentsForTeacher(String teacherId) {
        if (!hasText(teacherId)) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        String normalizedTeacherId = teacherId.trim();

        List<TeacherAssignedStudentDto> fromUserService = fetchAssignedStudentsFromUserService(normalizedTeacherId);
        if (!fromUserService.isEmpty()) {
            log.info("✅ Found {} assigned students from user service for teacherId={}",
                    fromUserService.size(), normalizedTeacherId);
            return fromUserService;
        }

        log.info("🔍 Falling back to bookings for assigned students, teacherId={}", normalizedTeacherId);

        List<Booking> teacherBookings = bookingRepository.findByTeacherId(normalizedTeacherId);

        if (teacherBookings == null || teacherBookings.isEmpty()) {
            log.info("ℹ️ No bookings found for teacherId={}", normalizedTeacherId);
            return List.of();
        }

        Map<String, TeacherAssignedStudentDto> studentMap = new LinkedHashMap<>();

        for (Booking booking : teacherBookings) {
            if (booking == null || !hasText(booking.getStudentId())) {
                continue;
            }

            if (booking.getStatus() != BookingStatus.PENDING
                    && booking.getStatus() != BookingStatus.PENDING_PAYMENT
                    && booking.getStatus() != BookingStatus.CONFIRMED
                    && booking.getStatus() != BookingStatus.COMPLETED) {
                continue;
            }

            String studentId = booking.getStudentId().trim();

            studentMap.putIfAbsent(
                    studentId,
                    TeacherAssignedStudentDto.builder()
                            .userId(studentId)
                            .studentId(studentId)
                            .name(hasText(booking.getStudentName()) ? booking.getStudentName().trim() : DEFAULT_STUDENT_NAME)
                            .email(hasText(booking.getStudentEmail()) ? booking.getStudentEmail().trim() : null)
                            .build()
            );
        }

        List<TeacherAssignedStudentDto> result = new ArrayList<>(studentMap.values());

        log.info("✅ Found {} assigned students for teacherId={}", result.size(), normalizedTeacherId);

        return result;
    }

    private List<TeacherAssignedStudentDto> fetchAssignedStudentsFromUserService(String teacherId) {
        if (!hasText(teacherId)) {
            return List.of();
        }

        try {
            List<AssignedStudentOptionDto> students = userServiceClient.getAssignedStudentsForTeacher(teacherId.trim());

            if (students == null || students.isEmpty()) {
                log.warn("⚠️ userServiceClient.getAssignedStudentsForTeacher returned empty for teacherId={}", teacherId);
                return List.of();
            }

            return students.stream()
                    .filter(Objects::nonNull)
                    .map(this::mapAssignedStudentOption)
                    .filter(Objects::nonNull)
                    .toList();

        } catch (Exception ex) {
            log.error("❌ Failed to fetch assigned students for teacherId={} error={}", teacherId, ex.getMessage(), ex);
            return List.of();
        }
    }

    private TeacherAssignedStudentDto mapAssignedStudentOption(AssignedStudentOptionDto student) {
        if (student == null) {
            return null;
        }

        String userId = firstNonBlank(student.getUserId(), student.getStudentId());
        if (!hasText(userId)) {
            return null;
        }

        return TeacherAssignedStudentDto.builder()
                .userId(userId.trim())
                .studentId(userId.trim())
                .name(hasText(student.getName()) ? student.getName().trim() : DEFAULT_STUDENT_NAME)
                .email(hasText(student.getEmail()) ? student.getEmail().trim() : null)
                .build();
    }

    @Transactional
    public List<BookingDto> assignStudentsToTeacherSlot(
            String teacherHeaderId,
            TeacherAssignStudentsBookingRequest request
    ) {
        log.info("📌 assignStudentsToTeacherSlot started");
        log.info("  - header teacher id: {}", teacherHeaderId);
        log.info("  - request teacher id: {}", request != null ? request.getTeacherId() : null);
        log.info("  - request student ids: {}", request != null ? request.getStudentUserIds() : null);
        log.info("  - request subject: {}", request != null ? request.getSubject() : null);
        log.info("  - request amount: {}", request != null ? request.getAmount() : null);
        log.info("  - request start: {}", request != null ? request.getSessionStartTime() : null);
        log.info("  - request end: {}", request != null ? request.getSessionEndTime() : null);

        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        String normalizedTeacherId = normalizeTeacherId(teacherHeaderId, request.getTeacherId());
        log.info("✅ normalizedTeacherId={}", normalizedTeacherId);

        if (request.getStudentUserIds() == null || request.getStudentUserIds().isEmpty()) {
            throw new IllegalArgumentException("At least one student must be selected");
        }

        List<String> normalizedStudentUserIds = request.getStudentUserIds().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .toList();

        log.info("✅ normalizedStudentUserIds={}", normalizedStudentUserIds);

        if (normalizedStudentUserIds.isEmpty()) {
            throw new IllegalArgumentException("At least one valid student must be selected");
        }

        if (!hasText(request.getSubject())) {
            throw new IllegalArgumentException("Subject is required");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        validateSessionRange(request.getSessionStartTime(), request.getSessionEndTime());
        validateBookingTime(request.getSessionStartTime());

        List<TeacherAssignedStudentDto> assignedStudents = getAssignedStudentsForTeacher(normalizedTeacherId);
        log.info("✅ assignedStudents count={} data={}", assignedStudents.size(), assignedStudents);

        Map<String, TeacherAssignedStudentDto> assignedStudentMap = assignedStudents.stream()
                .filter(Objects::nonNull)
                .filter(student -> hasText(student.getUserId()))
                .collect(Collectors.toMap(
                        student -> student.getUserId().trim(),
                        student -> student,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));

        log.info("✅ assignedStudentMap keys={}", assignedStudentMap.keySet());

        List<String> unassignedStudentIds = normalizedStudentUserIds.stream()
                .filter(studentUserId -> !assignedStudentMap.containsKey(studentUserId))
                .toList();

        if (!unassignedStudentIds.isEmpty()) {
            log.error("❌ Students {} are not assigned to teacher {}", unassignedStudentIds, normalizedTeacherId);
            throw new IllegalArgumentException("Selected students are not assigned to this teacher: " + unassignedStudentIds);
        }

        String slotLockKey = buildTeacherSlotLockKey(
                normalizedTeacherId,
                request.getSessionStartTime(),
                request.getSessionEndTime()
        );

        log.info("🔒 slotLockKey={}", slotLockKey);

        if (!lockService.acquireLock(slotLockKey, normalizedTeacherId)) {
            throw new IllegalArgumentException("Selected teacher slot is currently being processed");
        }

        try {
            ensureNoTeacherOverlapForManualAssignment(
                    normalizedTeacherId,
                    request.getSessionStartTime(),
                    request.getSessionEndTime(),
                    normalizedStudentUserIds
            );

            List<BookingDto> bookings = new ArrayList<>();

            for (String studentUserId : normalizedStudentUserIds) {
                TeacherAssignedStudentDto assignedStudent = assignedStudentMap.get(studentUserId);

                BookingDto bookingDto = createTeacherAssignedBookingWithoutTeacherOverlapCheck(
                        normalizedTeacherId,
                        request,
                        studentUserId,
                        assignedStudent != null && hasText(assignedStudent.getName())
                                ? assignedStudent.getName().trim()
                                : DEFAULT_STUDENT_NAME,
                        assignedStudent != null && hasText(assignedStudent.getEmail())
                                ? assignedStudent.getEmail().trim()
                                : null
                );

                bookings.add(bookingDto);
            }

            log.info("✅ Created {} bookings for teacher {}", bookings.size(), normalizedTeacherId);
            return bookings;

        } finally {
            lockService.releaseLock(slotLockKey, normalizedTeacherId);
            log.info("🔓 Released slot lock for teacher {}", normalizedTeacherId);
        }
    }

    private BookingDto createTeacherAssignedBookingWithoutTeacherOverlapCheck(
            String teacherId,
            TeacherAssignStudentsBookingRequest request,
            String studentUserId,
            String studentName,
            String studentEmail
    ) {
        if (!hasText(teacherId)) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        if (!hasText(studentUserId)) {
            throw new IllegalArgumentException("Student user ID is required");
        }

        if (!hasText(studentName)) {
            studentName = DEFAULT_STUDENT_NAME;
        }

        if (!hasText(studentEmail)) {
            throw new IllegalArgumentException("Student email is required for assigned booking");
        }

        Optional<Booking> existingBooking = bookingRepository
                .findByTeacherIdAndStudentIdAndSessionStartTimeAndSessionEndTime(
                        teacherId,
                        studentUserId,
                        request.getSessionStartTime(),
                        request.getSessionEndTime()
                );

        if (existingBooking.isPresent()) {
            log.warn("⚠️ Assigned booking already exists for teacherId={}, studentId={}, slot={} to {}",
                    teacherId,
                    studentUserId,
                    request.getSessionStartTime(),
                    request.getSessionEndTime());

            return toDto(existingBooking.get());
        }

        int duration = resolveDurationMinutes(
                request.getSessionStartTime(),
                request.getSessionEndTime()
        );

        ClassSession session = ClassSession.builder()
                .sessionType(SessionType.ONE_ON_ONE)
                .courseId(null)
                .teacherId(teacherId)
                .teacherName(hasText(request.getTeacherName()) ? request.getTeacherName().trim() : DEFAULT_TEACHER_NAME)
                .studentId(studentUserId)
                .bookingId(null)
                .title(hasText(request.getSubject()) ? request.getSubject().trim() : "Assigned Class")
                .description("Teacher assigned class for " + studentName)
                .status(ClassStatus.SCHEDULED)
                .scheduledStartTime(request.getSessionStartTime())
                .scheduledEndTime(request.getSessionEndTime())
                .durationMinutes(duration)
                .maxParticipants(1)
                .participants(new ArrayList<>())
                .attendedCount(0)
                .materialUrls(new ArrayList<>())
                .reminderSent(false)
                .createdBy(teacherId)
                .build();

        ClassSession savedSession = sessionRepository.save(session);

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        Booking booking = Booking.builder()
                .sessionId(savedSession.getId())
                .courseId(null)
                .studentId(studentUserId)
                .studentName(studentName)
                .studentEmail(studentEmail)
                .teacherId(teacherId)
                .teacherName(savedSession.getTeacherName())
                .parentId(null)
                .subject(request.getSubject().trim())
                .durationMinutes(duration)
                .sessionStartTime(request.getSessionStartTime())
                .sessionEndTime(request.getSessionEndTime())
                .status(BookingStatus.PENDING_PAYMENT)
                .amount(request.getAmount())
                .currency(defaultCurrency(request.getCurrency()))
                .bookedAt(now)
                .cancellationPolicy(getDefaultCancellationPolicy())
                .reminderSent(false)
                .notes(request.getNotes())
                .createdAt(now)
                .updatedAt(now)
                .isFreeDemo(false)
                .freeSlotsApplied(0)
                .paidSlotsApplied(1)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        savedSession.setBookingId(savedBooking.getId());
        sessionRepository.save(savedSession);

        eventPublisher.publishBookingCreated(savedBooking);

        log.info("✅ Teacher assigned booking created. bookingId={}, sessionId={}, teacherId={}, studentId={}, status={}",
                savedBooking.getId(),
                savedSession.getId(),
                teacherId,
                studentUserId,
                savedBooking.getStatus());

        return toDto(savedBooking);
    }

    private void ensureNoTeacherOverlapForManualAssignment(
            String teacherId,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            List<String> requestedStudentUserIds
    ) {
        List<ClassSession> overlappingSessions =
                sessionRepository.findOverlappingSessions(
                        teacherId,
                        requestedStart,
                        requestedEnd
                );

        if (overlappingSessions == null || overlappingSessions.isEmpty()) {
            return;
        }

        Set<String> requestedStudents = requestedStudentUserIds == null
                ? Set.of()
                : requestedStudentUserIds.stream()
                .filter(this::hasText)
                .map(String::trim)
                .collect(Collectors.toSet());

        for (ClassSession session : overlappingSessions) {
            if (session == null) {
                continue;
            }

            boolean sameExactSlot =
                    Objects.equals(session.getScheduledStartTime(), requestedStart) &&
                            Objects.equals(session.getScheduledEndTime(), requestedEnd);

            boolean sameStudentAssignment =
                    hasText(session.getStudentId()) &&
                            requestedStudents.contains(session.getStudentId().trim());

            if (sameExactSlot && sameStudentAssignment) {
                continue;
            }

            throw new IllegalArgumentException("Time slot already booked for this teacher");
        }
    }

    private BookingDto applyDisplayTimezone(BookingDto dto, String timezoneId) {
        String safeTimezoneId = hasText(timezoneId) ? timezoneId : APP_ZONE.getId();
        ZoneId teacherZone = ZoneId.of(safeTimezoneId);

        if (dto.getSessionStartTime() != null) {
            ZonedDateTime z = dto.getSessionStartTime().atZone(teacherZone);
            dto.setDisplaySessionStartTime(z.format(DISPLAY_FMT));
        }

        if (dto.getSessionEndTime() != null) {
            ZonedDateTime z = dto.getSessionEndTime().atZone(teacherZone);
            dto.setDisplaySessionEndTime(z.format(DISPLAY_FMT));
        }

        if (dto.getBookedAt() != null) {
            ZonedDateTime z = dto.getBookedAt().atZone(teacherZone);
            dto.setDisplayBookedAt(z.format(DISPLAY_FMT));
        }

        dto.setDisplayTimezoneId(safeTimezoneId);
        dto.setDisplayTimezoneAbbreviation(
                ZonedDateTime.now(teacherZone).format(DateTimeFormatter.ofPattern("z"))
        );

        return dto;
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
                .subjectName(booking.getSubjectName())
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
                .freeSlotsApplied(booking.getFreeSlotsApplied())
                .paidSlotsApplied(booking.getPaidSlotsApplied())
                .build();
    }

    private BookingDto toDtoWithVideoSession(Booking booking) {
        Booking effectiveBooking = booking;
        BookingDto dto = toDto(effectiveBooking);

        if (booking == null || !hasText(booking.getId())) {
            return dto;
        }

        try {
            VideoSessionCreateResponse videoSession = videoServiceClient.getSessionByBookingId(booking.getId());

            if (videoSession != null) {
                effectiveBooking = syncBookingStatusFromVideoSession(booking, videoSession.getStatus());
                dto = toDto(effectiveBooking);
                dto.setVideoSession(videoSession);
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not fetch video session for booking {}: {}", booking.getId(), e.getMessage());
        }

        return dto;
    }

    @Transactional
    protected Booking syncBookingStatusFromVideoSession(Booking booking, String videoStatus) {
        if (booking == null || !hasText(videoStatus)) {
            return booking;
        }

        BookingStatus targetStatus = mapVideoStatusToBookingStatus(videoStatus);
        if (targetStatus == null) {
            return booking;
        }

        if (booking.getStatus() == targetStatus) {
            return booking;
        }

        if (booking.getStatus() == BookingStatus.REJECTED || booking.getStatus() == BookingStatus.CANCELLED) {
            return booking;
        }

        if (booking.getStatus() == BookingStatus.COMPLETED || booking.getStatus() == BookingStatus.NO_SHOW) {
            return booking;
        }

        LocalDateTime now = LocalDateTime.now(APP_ZONE);

        booking.setStatus(targetStatus);
        booking.setUpdatedAt(now);

        if (targetStatus == BookingStatus.COMPLETED && booking.getCompletedAt() == null) {
            booking.setCompletedAt(now);
        }

        if (targetStatus == BookingStatus.CANCELLED && booking.getCancelledAt() == null) {
            booking.setCancelledAt(now);
        }

        Booking saved = bookingRepository.save(booking);
        log.info("✅ Synced booking {} status from video session: {}", saved.getId(), targetStatus);

        return saved;
    }

    private BookingStatus mapVideoStatusToBookingStatus(String videoStatus) {
        switch (videoStatus) {
            case "COMPLETED":
                return BookingStatus.COMPLETED;
            case "NO_SHOW":
            case "NOSHOW":
                return BookingStatus.NO_SHOW;
            case "CANCELLED":
                return BookingStatus.CANCELLED;
            default:
                return null;
        }
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeTeacherId(String teacherHeaderId, String requestTeacherId) {
        if (!hasText(teacherHeaderId)) {
            throw new IllegalArgumentException("Teacher header ID is required");
        }
        if (!hasText(requestTeacherId)) {
            throw new IllegalArgumentException("Teacher ID is required");
        }

        String normalizedHeaderTeacherId = teacherHeaderId.trim();
        String normalizedRequestTeacherId = requestTeacherId.trim();

        if (!normalizedHeaderTeacherId.equals(normalizedRequestTeacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this slot");
        }

        return normalizedRequestTeacherId;
    }

    private String reasonOrDefault(String reason) {
        return hasText(reason) ? reason.trim() : "Cancelled by user";
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }

        return null;
    }
}