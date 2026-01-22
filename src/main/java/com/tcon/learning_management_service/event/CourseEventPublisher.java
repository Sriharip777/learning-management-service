package com.tcon.learning_management_service.event;


import com.tcon.learning_management_service.course.entity.Course;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "course-events";

    public void publishCourseCreated(Course course) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType("COURSE_CREATED")
                    .courseId(course.getId())
                    .teacherId(course.getTeacherId())
                    .title(course.getTitle())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, course.getId(), event);
            log.info("Published course created event: {}", course.getId());
        } catch (Exception e) {
            log.error("Failed to publish course created event", e);
        }
    }

    public void publishCourseUpdated(Course course) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType("COURSE_UPDATED")
                    .courseId(course.getId())
                    .teacherId(course.getTeacherId())
                    .title(course.getTitle())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, course.getId(), event);
            log.info("Published course updated event: {}", course.getId());
        } catch (Exception e) {
            log.error("Failed to publish course updated event", e);
        }
    }

    public void publishCoursePublished(Course course) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType("COURSE_PUBLISHED")
                    .courseId(course.getId())
                    .teacherId(course.getTeacherId())
                    .title(course.getTitle())
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, course.getId(), event);
            log.info("Published course published event: {}", course.getId());
        } catch (Exception e) {
            log.error("Failed to publish course published event", e);
        }
    }

    public void publishCourseDeleted(String courseId) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType("COURSE_DELETED")
                    .courseId(courseId)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, courseId, event);
            log.info("Published course deleted event: {}", courseId);
        } catch (Exception e) {
            log.error("Failed to publish course deleted event", e);
        }
    }

    public void publishStudentEnrolled(String courseId, String studentId, String studentName) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType("STUDENT_ENROLLED")
                    .courseId(courseId)
                    .studentId(studentId)
                    .studentName(studentName)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, courseId, event);
            log.info("Published student enrolled event: {} in course {}", studentId, courseId);
        } catch (Exception e) {
            log.error("Failed to publish student enrolled event", e);
        }
    }

    public void publishStudentUnenrolled(String courseId, String studentId) {
        try {
            CourseEvent event = CourseEvent.builder()
                    .eventType("STUDENT_UNENROLLED")
                    .courseId(courseId)
                    .studentId(studentId)
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, courseId, event);
            log.info("Published student unenrolled event: {} from course {}", studentId, courseId);
        } catch (Exception e) {
            log.error("Failed to publish student unenrolled event", e);
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CourseEvent {
        private String eventType;
        private String courseId;
        private String teacherId;
        private String title;
        private String studentId;
        private String studentName;
        private java.time.LocalDateTime timestamp;
    }
}
