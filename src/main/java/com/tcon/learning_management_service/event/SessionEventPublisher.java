package com.tcon.learning_management_service.event;

import com.tcon.events.events.SessionEvent;
import com.tcon.learning_management_service.demo.entity.DemoClass;
import com.tcon.learning_management_service.session.entity.ClassSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventPublisher {

    private static final String TOPIC = "session-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishSessionScheduled(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_SCHEDULED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .scheduledStartTime(session.getScheduledStartTime())
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session scheduled event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session scheduled event: {}", session != null ? session.getId() : null, e);
        }
    }

    public void publishSessionCreated(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_CREATED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .scheduledStartTime(session.getScheduledStartTime())
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published SESSION_CREATED event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session created event: {}", session != null ? session.getId() : null, e);
        }
    }

    public void publishSessionStarted(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_STARTED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session started event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session started event: {}", session != null ? session.getId() : null, e);
        }
    }

    public void publishSessionCompleted(ClassSession session) {
        try {
            SessionEvent event = SessionEvent.builder()
                    .eventType("SESSION_COMPLETED")
                    .sessionId(session.getId())
                    .courseId(session.getCourseId())
                    .teacherId(session.getTeacherId())
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session completed event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session completed event: {}", session != null ? session.getId() : null, e);
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
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session cancelled event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session cancelled event: {}", session != null ? session.getId() : null, e);
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
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, oldSession.getId(), event);
            log.info("Published session rescheduled event: {} -> {}", oldSession.getId(), newSession.getId());
        } catch (Exception e) {
            log.error("Failed to publish session rescheduled event: {} -> {}",
                    oldSession != null ? oldSession.getId() : null,
                    newSession != null ? newSession.getId() : null,
                    e);
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
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, session.getId(), event);
            log.info("Published session reminder event: {}", session.getId());
        } catch (Exception e) {
            log.error("Failed to publish session reminder event: {}", session != null ? session.getId() : null, e);
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
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, demo.getId(), event);
            log.info("Published demo class scheduled event: {}", demo.getId());
        } catch (Exception e) {
            log.error("Failed to publish demo class scheduled event: {}", demo != null ? demo.getId() : null, e);
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
                    .timestamp(Instant.now())
                    .build();

            kafkaTemplate.send(TOPIC, demo.getId(), event);
            log.info("Published demo class completed event: {}", demo.getId());
        } catch (Exception e) {
            log.error("Failed to publish demo class completed event: {}", demo != null ? demo.getId() : null, e);
        }
    }
}