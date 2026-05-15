package com.tcon.learning_management_service.event;

import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.CourseSchedule;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.entity.SessionType;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventListener {

    private final CourseRepository courseRepository;
    private final ClassSessionRepository sessionRepository;
    private final SessionEventPublisher sessionEventPublisher;

    @KafkaListener(topics = "course-events", groupId = "learning-management-service")
    @Transactional
    public void handleCourseEvent(Map<String, Object> event) {
        String eventType = (String) event.get("eventType");
        log.info("📨 Received course event: {}", eventType);

        switch (eventType) {
            case "COURSE_PUBLISHED":
                handleCoursePublished(event);
                break;
            case "COURSE_DELETED":
                handleCourseDeleted(event);
                break;
            default:
                log.debug("Ignoring event type: {}", eventType);
        }
    }

    private void handleCoursePublished(Map<String, Object> event) {
        String courseId = (String) event.get("courseId");
        log.info("🎓 Processing COURSE_PUBLISHED for courseId: {}", courseId);

        try {
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

            List<ClassSession> sessions = generateSessionsForCourse(course);

            log.info("✅ Generated {} sessions for course: {}", sessions.size(), courseId);

            for (ClassSession session : sessions) {
                sessionEventPublisher.publishSessionCreated(session);
            }

        } catch (Exception e) {
            log.error("❌ Failed to generate sessions for course: {}", courseId, e);
        }
    }

    private void handleCourseDeleted(Map<String, Object> event) {
        String courseId = (String) event.get("courseId");
        log.info("🗑️ Processing COURSE_DELETED for courseId: {}", courseId);

        try {
            List<ClassSession> sessions = sessionRepository.findByCourseId(courseId);

            Instant now = Instant.now();
            for (ClassSession session : sessions) {
                if (session.getScheduledStartTime().isAfter(now) &&
                        session.getStatus() == ClassStatus.SCHEDULED) {

                    session.setStatus(ClassStatus.CANCELLED);
                    session.setCancellationReason("Course deleted by teacher");
                    sessionRepository.save(session);

                    sessionEventPublisher.publishSessionCancelled(session);
                }
            }

            log.info("✅ Cancelled {} future sessions for deleted course", sessions.size());

        } catch (Exception e) {
            log.error("❌ Failed to cancel sessions for deleted course: {}", courseId, e);
        }
    }

    private List<ClassSession> generateSessionsForCourse(Course course) {
        log.info("🔨 Generating sessions for course: {}", course.getTitle());
        log.info("📅 Schedule: {}", course.getSchedule());
        log.info("📊 Total sessions: {}", course.getTotalSessions());

        List<ClassSession> sessions = new ArrayList<>();

        CourseSchedule schedule = course.getSchedule();

        if (schedule == null || schedule.getDaysOfWeek() == null || schedule.getDaysOfWeek().isEmpty()) {
            log.warn("⚠️ No schedule found for course: {}", course.getId());
            return sessions;
        }

        List<DayOfWeek> daysOfWeek = schedule.getDaysOfWeek();
        LocalTime startTime = schedule.getStartTime() != null ? schedule.getStartTime() : LocalTime.of(10, 0);
        LocalTime endTime = schedule.getEndTime() != null ? schedule.getEndTime() : LocalTime.of(11, 0);

        Instant currentDate = course.getStartDate().atTime(startTime).toInstant(ZoneOffset.UTC);
        Instant endDate = course.getEndDate().atTime(23, 59).toInstant(ZoneOffset.UTC);

        int sessionCount = 0;
        int maxSessions = course.getTotalSessions();

        while (sessionCount < maxSessions && currentDate.isBefore(endDate)) {

            if (daysOfWeek.contains(currentDate.atOffset(ZoneOffset.UTC).getDayOfWeek())) {

                Instant sessionStart = currentDate;
                Instant sessionEnd = currentDate.atOffset(ZoneOffset.UTC)
                        .withHour(endTime.getHour())
                        .withMinute(endTime.getMinute())
                        .withSecond(0)
                        .withNano(0)
                        .toInstant();

                int durationMinutes = (int) Duration.between(sessionStart, sessionEnd).toMinutes();

                ClassSession session = ClassSession.builder()
                        .sessionType(SessionType.REGULAR)
                        .courseId(course.getId())
                        .teacherId(course.getTeacherId())
                        .teacherName("")
                        .title(course.getTitle() + " - Session " + (sessionCount + 1))
                        .description("Group session for " + course.getTitle())
                        .status(ClassStatus.SCHEDULED)
                        .scheduledStartTime(sessionStart)
                        .scheduledEndTime(sessionEnd)
                        .durationMinutes(durationMinutes)
                        .maxParticipants(course.getMaxStudents())
                        .participants(new ArrayList<>())
                        .attendedCount(0)
                        .materialUrls(new ArrayList<>())
                        .reminderSent(false)
                        .createdBy(course.getTeacherId())
                        .build();

                ClassSession saved = sessionRepository.save(session);
                sessions.add(saved);

                sessionCount++;

                log.info("✅ Created session {}/{}: {} at {}",
                        sessionCount, maxSessions, saved.getId(), sessionStart);
            }

            currentDate = currentDate.plus(Duration.ofDays(1));
        }

        return sessions;
    }
}