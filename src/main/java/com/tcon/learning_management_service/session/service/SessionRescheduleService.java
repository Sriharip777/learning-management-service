package com.tcon.learning_management_service.session.service;

import com.tcon.learning_management_service.event.SessionEventPublisher;
import com.tcon.learning_management_service.session.dto.SessionDto;
import com.tcon.learning_management_service.session.dto.SessionRescheduleRequest;
import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRescheduleService {

    private final ClassSessionRepository sessionRepository;
    private final SessionEventPublisher eventPublisher;

    @Transactional
    public SessionDto rescheduleSession(String sessionId, String teacherId,
                                        SessionRescheduleRequest request) {
        log.info("Rescheduling session: {}", sessionId);

        ClassSession oldSession = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!oldSession.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this session");
        }

        if (oldSession.getStatus() != ClassStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only scheduled sessions can be rescheduled");
        }

        // Calculate new end time
        LocalDateTime newEndTime = request.getNewScheduledStartTime()
                .plusMinutes(oldSession.getDurationMinutes());

        // Check for conflicts
        List<ClassSession> conflicts = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                teacherId,
                request.getNewScheduledStartTime().minusMinutes(oldSession.getDurationMinutes()),
                newEndTime
        );

        conflicts.removeIf(s -> s.getId().equals(sessionId)); // Exclude current session

        if (!conflicts.isEmpty()) {
            throw new IllegalArgumentException("New time slot conflicts with existing sessions");
        }

        // Create new session
        ClassSession newSession = ClassSession.builder()
                .courseId(oldSession.getCourseId())
                .teacherId(oldSession.getTeacherId())
                .teacherName(oldSession.getTeacherName())
                .title(oldSession.getTitle())
                .description(oldSession.getDescription())
                .sessionType(oldSession.getSessionType())
                .status(ClassStatus.SCHEDULED)
                .scheduledStartTime(request.getNewScheduledStartTime())
                .scheduledEndTime(newEndTime)
                .durationMinutes(oldSession.getDurationMinutes())
                .meetingUrl(oldSession.getMeetingUrl())
                .meetingId(oldSession.getMeetingId())
                .meetingPassword(oldSession.getMeetingPassword())
                .maxParticipants(oldSession.getMaxParticipants())
                .participants(oldSession.getParticipants())
                .materialUrls(oldSession.getMaterialUrls())
                .notes(oldSession.getNotes())
                .rescheduledFromId(sessionId)
                .rescheduleReason(request.getReason())
                .rescheduledAt(LocalDateTime.now())
                .reminderSent(false)
                .createdBy(teacherId)
                .build();

        ClassSession savedNewSession = sessionRepository.save(newSession);

        // Update old session status
        oldSession.setStatus(ClassStatus.RESCHEDULED);
        oldSession.setRescheduledToId(savedNewSession.getId());
        oldSession.setRescheduleReason(request.getReason());
        oldSession.setRescheduledAt(LocalDateTime.now());
        sessionRepository.save(oldSession);

        log.info("Session rescheduled successfully. Old: {}, New: {}", sessionId, savedNewSession.getId());

        // Publish event
        eventPublisher.publishSessionRescheduled(oldSession, savedNewSession);

        return toDto(savedNewSession);
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
