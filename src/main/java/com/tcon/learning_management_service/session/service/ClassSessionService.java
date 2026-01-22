package com.tcon.learning_management_service.session.service;

import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import com.tcon.learning_management_service.event.SessionEventPublisher;
import com.tcon.learning_management_service.session.dto.SessionDto;
import com.tcon.learning_management_service.session.dto.SessionScheduleRequest;
import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.entity.SessionParticipant;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository sessionRepository;
    private final CourseRepository courseRepository;
    private final SessionEventPublisher eventPublisher;

    @Transactional
    public SessionDto scheduleSession(String teacherId, SessionScheduleRequest request) {
        log.info("Scheduling session for course: {} by teacher: {}", request.getCourseId(), teacherId);

        // Validate course exists and teacher owns it
        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + request.getCourseId()));

        if (!course.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this course");
        }

        // Calculate end time
        LocalDateTime scheduledEndTime = request.getScheduledStartTime()
                .plusMinutes(request.getDurationMinutes());

        // Check for conflicts
        List<ClassSession> conflicts = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                teacherId,
                request.getScheduledStartTime().minusMinutes(request.getDurationMinutes()),
                scheduledEndTime
        );

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("Session conflicts with existing sessions");
        }

        ClassSession session = ClassSession.builder()
                .courseId(request.getCourseId())
                .teacherId(teacherId)
                .teacherName(course.getTitle()) // Should get from teacher service
                .title(request.getTitle())
                .description(request.getDescription())
                .sessionType(request.getSessionType())
                .status(ClassStatus.SCHEDULED)
                .scheduledStartTime(request.getScheduledStartTime())
                .scheduledEndTime(scheduledEndTime)
                .durationMinutes(request.getDurationMinutes())
                .meetingUrl(request.getMeetingUrl())
                .meetingId(request.getMeetingId())
                .meetingPassword(request.getMeetingPassword())
                .maxParticipants(request.getMaxParticipants() != null ?
                        request.getMaxParticipants() : course.getMaxStudents())
                .participants(new ArrayList<>())
                .attendedCount(0)
                .materialUrls(request.getMaterialUrls() != null ?
                        request.getMaterialUrls() : new ArrayList<>())
                .notes(request.getNotes())
                .reminderSent(false)
                .createdBy(teacherId)
                .build();

        ClassSession saved = sessionRepository.save(session);
        log.info("Session scheduled successfully: {}", saved.getId());

        // Publish event
        eventPublisher.publishSessionScheduled(saved);

        return toDto(saved);
    }

    public SessionDto getSession(String sessionId) {
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        return toDto(session);
    }

    public List<SessionDto> getCourseSessions(String courseId) {
        return sessionRepository.findByCourseId(courseId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<SessionDto> getTeacherSessions(String teacherId) {
        return sessionRepository.findByTeacherId(teacherId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<SessionDto> getStudentSessions(String studentId) {
        return sessionRepository.findByStudentId(studentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<SessionDto> getTeacherSessionsInDateRange(
            String teacherId, LocalDateTime start, LocalDateTime end) {
        return sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(teacherId, start, end)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SessionDto startSession(String sessionId, String teacherId) {
        log.info("Starting session: {}", sessionId);

        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this session");
        }

        if (session.getStatus() != ClassStatus.SCHEDULED) {
            throw new IllegalArgumentException("Session is not in scheduled state");
        }

        session.setStatus(ClassStatus.IN_PROGRESS);
        session.setActualStartTime(LocalDateTime.now());

        ClassSession updated = sessionRepository.save(session);
        log.info("Session started: {}", sessionId);

        // Publish event
        eventPublisher.publishSessionStarted(updated);

        return toDto(updated);
    }

    @Transactional
    public SessionDto completeSession(String sessionId, String teacherId, String notes) {
        log.info("Completing session: {}", sessionId);

        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this session");
        }

        if (session.getStatus() != ClassStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Session is not in progress");
        }

        session.setStatus(ClassStatus.COMPLETED);
        session.setActualEndTime(LocalDateTime.now());
        if (notes != null) {
            session.setNotes(notes);
        }

        ClassSession updated = sessionRepository.save(session);
        log.info("Session completed: {}", sessionId);

        // Publish event
        eventPublisher.publishSessionCompleted(updated);

        return toDto(updated);
    }

    @Transactional
    public SessionDto cancelSession(String sessionId, String teacherId, String reason) {
        log.info("Cancelling session: {}", sessionId);

        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this session");
        }

        if (session.getStatus() == ClassStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel completed session");
        }

        session.setStatus(ClassStatus.CANCELLED);
        session.setCancellationReason(reason);
        session.setCancelledAt(LocalDateTime.now());
        session.setCancelledBy(teacherId);

        ClassSession updated = sessionRepository.save(session);
        log.info("Session cancelled: {}", sessionId);

        // Publish event
        eventPublisher.publishSessionCancelled(updated);

        return toDto(updated);
    }

    @Transactional
    public SessionDto markStudentAttendance(String sessionId, String studentId,
                                            String studentName, String studentEmail, boolean attended) {
        log.info("Marking attendance for student {} in session {}: {}", studentId, sessionId, attended);

        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // Find or create participant
        SessionParticipant participant = session.getParticipants().stream()
                .filter(p -> p.getStudentId().equals(studentId))
                .findFirst()
                .orElse(SessionParticipant.builder()
                        .studentId(studentId)
                        .studentName(studentName)
                        .studentEmail(studentEmail)
                        .build());

        participant.setAttended(attended);
        if (attended) {
            participant.setJoinedAt(LocalDateTime.now());
        }

        // Update or add participant
        session.getParticipants().removeIf(p -> p.getStudentId().equals(studentId));
        session.getParticipants().add(participant);

        // Update attended count
        long attendedCount = session.getParticipants().stream()
                .filter(p -> Boolean.TRUE.equals(p.getAttended()))
                .count();
        session.setAttendedCount((int) attendedCount);

        ClassSession updated = sessionRepository.save(session);
        log.info("Attendance marked successfully");

        return toDto(updated);
    }

    @Transactional
    public SessionDto addRecording(String sessionId, String teacherId, String recordingUrl) {
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this session");
        }

        session.setRecordingUrl(recordingUrl);
        ClassSession updated = sessionRepository.save(session);

        log.info("Recording added to session: {}", sessionId);
        return toDto(updated);
    }

    private SessionDto toDto(ClassSession session) {
        return SessionDto.builder()
                .id(session.getId())
                .courseId(session.getCourseId())
                .teacherId(session.getTeacherId())
                .teacherName(session.getTeacherName())
                .title(session.getTitle())
                .description(session.getDescription())
                .sessionType(session.getSessionType())
                .status(session.getStatus())
                .scheduledStartTime(session.getScheduledStartTime())
                .scheduledEndTime(session.getScheduledEndTime())
                .actualStartTime(session.getActualStartTime())
                .actualEndTime(session.getActualEndTime())
                .durationMinutes(session.getDurationMinutes())
                .meetingUrl(session.getMeetingUrl())
                .meetingId(session.getMeetingId())
                .meetingPassword(session.getMeetingPassword())
                .participants(session.getParticipants())
                .maxParticipants(session.getMaxParticipants())
                .attendedCount(session.getAttendedCount())
                .recordingUrl(session.getRecordingUrl())
                .materialUrls(session.getMaterialUrls())
                .notes(session.getNotes())
                .rescheduledFromId(session.getRescheduledFromId())
                .rescheduledToId(session.getRescheduledToId())
                .rescheduleReason(session.getRescheduleReason())
                .rescheduledAt(session.getRescheduledAt())
                .cancellationReason(session.getCancellationReason())
                .cancelledAt(session.getCancelledAt())
                .cancelledBy(session.getCancelledBy())
                .reminderSent(session.getReminderSent())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }
}
