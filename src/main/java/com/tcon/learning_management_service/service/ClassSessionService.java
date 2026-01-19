package com.tcon.learning_management_service.service;


import com.tcon.learning_management_service.dto.SessionDto;
import com.tcon.learning_management_service.dto.SessionScheduleRequest;
import com.tcon.learning_management_service.entity.ClassSession;
import com.tcon.learning_management_service.entity.ClassStatus;
import com.tcon.learning_management_service.event.SessionEventPublisher;
import com.tcon.learning_management_service.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClassSessionService {

    private final ClassSessionRepository sessionRepository;
    private final SessionEventPublisher sessionEventPublisher;

    public SessionDto scheduleSession(SessionScheduleRequest request) {
        log.info("Scheduling session for course: {}", request.getCourseId());

        ClassSession session = ClassSession.builder()
                .courseId(request.getCourseId())
                .bookingId(request.getBookingId())
                .teacherId(request.getTeacherId())
                .studentId(request.getStudentId())
                .scheduledStartTime(request.getScheduledStartTime())
                .scheduledEndTime(request.getScheduledEndTime())
                .status(ClassStatus.SCHEDULED)
                .reminderSent(false)
                .build();

        ClassSession savedSession = sessionRepository.save(session);

        // Publish session scheduled event
        sessionEventPublisher.publishSessionScheduled(savedSession);

        return convertToDto(savedSession);
    }

    public SessionDto startSession(String sessionId) {
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        session.setStatus(ClassStatus.IN_PROGRESS);
        session.setActualStartTime(LocalDateTime.now());

        ClassSession updatedSession = sessionRepository.save(session);

        // Publish session started event
        sessionEventPublisher.publishSessionStarted(updatedSession);

        return convertToDto(updatedSession);
    }

    public SessionDto completeSession(String sessionId) {
        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        session.setStatus(ClassStatus.COMPLETED);
        session.setActualEndTime(LocalDateTime.now());

        ClassSession updatedSession = sessionRepository.save(session);

        // Publish session completed event
        sessionEventPublisher.publishSessionCompleted(updatedSession);

        return convertToDto(updatedSession);
    }

    public List<SessionDto> getSessionsByTeacher(String teacherId) {
        return sessionRepository.findByTeacherId(teacherId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<SessionDto> getSessionsByStudent(String studentId) {
        return sessionRepository.findByStudentId(studentId)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<SessionDto> getUpcomingSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime futureDate = now.plusDays(7);

        return sessionRepository.findByScheduledStartTimeBetween(now, futureDate)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    private SessionDto convertToDto(ClassSession session) {
        return SessionDto.builder()
                .id(session.getId())
                .courseId(session.getCourseId())
                .bookingId(session.getBookingId())
                .teacherId(session.getTeacherId())
                .studentId(session.getStudentId())
                .scheduledStartTime(session.getScheduledStartTime())
                .scheduledEndTime(session.getScheduledEndTime())
                .actualStartTime(session.getActualStartTime())
                .actualEndTime(session.getActualEndTime())
                .status(session.getStatus())
                .videoRoomId(session.getVideoRoomId())
                .recordingUrl(session.getRecordingUrl())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
