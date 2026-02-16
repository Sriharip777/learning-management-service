package com.tcon.learning_management_service.event;
import com.tcon.learning_management_service.demo.entity.DemoClass;
import com.tcon.learning_management_service.session.entity.ClassSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "session-events";

    public void publishSessionScheduled(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_SCHEDULED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .scheduledStartTime(session.getScheduledStartTime())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session scheduled event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session scheduled event", e);
        }
    }

    // ✅ ADD THIS NEW METHOD
    public void publishSessionCreated(ClassSession session) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "SESSION_CREATED");
            event.put("sessionId", session.getId());
            event.put("courseId", session.getCourseId());
            event.put("sessionType", session.getSessionType().toString());
            event.put("teacherId", session.getTeacherId());
            event.put("scheduledStartTime", session.getScheduledStartTime().toString());
            event.put("scheduledEndTime", session.getScheduledEndTime().toString());
            event.put("durationMinutes", session.getDurationMinutes());
            event.put("maxParticipants", session.getMaxParticipants());
            event.put("title", session.getTitle());
            event.put("timestamp", LocalDateTime.now().toString());

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("📤 Published SESSION_CREATED event: {}", session.getId());
        } catch (Exception e) {
            log.error("❌ Failed to publish session created event", e);
        }
    }


    public void publishSessionStarted(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_STARTED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session started event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session started event", e);
        }
    }

    public void publishSessionCompleted(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_COMPLETED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session completed event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session completed event", e);
        }
    }

    public void publishSessionCancelled(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_CANCELLED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .cancellationReason(session.getCancellationReason())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session cancelled event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session cancelled event", e);
        }
    }

    public void publishSessionRescheduled(ClassSession oldSession, ClassSession newSession) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_RESCHEDULED")
                    .sessionId(oldSession.getId())
                    .newSessionId(newSession.getId())
                    .courseId(oldSession.getCourseId())
                    .teacherId(oldSession.getTeacherId())
                    .scheduledStartTime(newSession.getScheduledStartTime())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, oldSession.getId(), event);
            log.info("Published session rescheduled event: {} -> {}", oldSession.getId(), newSession.getId());
        } catch (Exception e) {
            log.error("Failed to publish session rescheduled event", e);
        }
    }

    public void publishSessionReminder(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_REMINDER")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .scheduledStartTime(session.getScheduledStartTime())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session reminder event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session reminder event", e);
        }
    }

    public void publishDemoClassScheduled(DemoClass demo) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("DEMO_CLASS_SCHEDULED")
                    .sessionId(demo.getId())
                    .courseId(demo.getCourseId())
                    .teacherId(demo.getTeacherId())
                    .studentId(demo.getStudentId())
                    .scheduledStartTime(demo.getScheduledStartTime())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, demo.getId(), event);
            log.info("Published demo class scheduled event: {}", demo.getId());
        } catch (Exception e) {
            log.error("Failed to publish demo class scheduled event", e);
        }
    }

    public void publishDemoClassCompleted(DemoClass demo) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("DEMO_CLASS_COMPLETED")
                    .sessionId(demo.getId())
                    .courseId(demo.getCourseId())
                    .teacherId(demo.getTeacherId())
                    .studentId(demo.getStudentId())
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, demo.getId(), event);
            log.info("Published demo class completed event: {}", demo.getId());
        } catch (Exception e) {
            log.error("Failed to publish demo class completed event", e);
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SessionEvent {
        private String eventType;
        private String sessionId;
        private String newSessionId;
        private String courseId;
        private String teacherId;
        private String studentId;
        private LocalDateTime scheduledStartTime;
        private String cancellationReason;
        private LocalDateTime timestamp;
    }
}