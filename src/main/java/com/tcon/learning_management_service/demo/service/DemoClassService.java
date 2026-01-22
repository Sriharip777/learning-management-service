package com.tcon.learning_management_service.demo.service;


import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import com.tcon.learning_management_service.demo.dto.DemoClassDto;
import com.tcon.learning_management_service.demo.entity.DemoClass;
import com.tcon.learning_management_service.demo.repository.DemoClassRepository;
import com.tcon.learning_management_service.event.SessionEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoClassService {

    private final DemoClassRepository demoRepository;
    private final CourseRepository courseRepository;
    private final DemoLimitService limitService;
    private final SessionEventPublisher eventPublisher;

    @Transactional
    public DemoClassDto scheduleDemoClass(String studentId, String studentName, String studentEmail,
                                          String courseId, LocalDateTime scheduledStartTime,
                                          String studentNotes) {
        log.info("Scheduling demo class for student {} and course {}", studentId, courseId);

        // Check demo limit
        if (!limitService.canBookDemo(studentId)) {
            throw new IllegalArgumentException("Student has reached the maximum demo class limit");
        }

        // Get course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // Check if course offers demo
        if (!Boolean.TRUE.equals(course.getIsDemoAvailable())) {
            throw new IllegalArgumentException("This course does not offer demo classes");
        }

        // Check if student already has a demo for this course
        if (demoRepository.existsByStudentIdAndCourseIdAndStatus(
                studentId, courseId, DemoClass.DemoStatus.SCHEDULED)) {
            throw new IllegalArgumentException("Student already has a scheduled demo for this course");
        }

        // Validate time is in future
        if (scheduledStartTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Demo class must be scheduled in the future");
        }

        Integer duration = course.getDemoSessionDuration() != null ?
                course.getDemoSessionDuration() : 30;

        LocalDateTime scheduledEndTime = scheduledStartTime.plusMinutes(duration);

        // Create demo class
        DemoClass demoClass = DemoClass.builder()
                .studentId(studentId)
                .studentName(studentName)
                .studentEmail(studentEmail)
                .teacherId(course.getTeacherId())
                .teacherName(course.getTitle()) // Should get teacher name from teacher service
                .courseId(courseId)
                .courseName(course.getTitle())
                .status(DemoClass.DemoStatus.SCHEDULED)
                .scheduledStartTime(scheduledStartTime)
                .scheduledEndTime(scheduledEndTime)
                .durationMinutes(duration)
                .studentNotes(studentNotes)
                .convertedToFullCourse(false)
                .reminderSent(false)
                .build();

        DemoClass saved = demoRepository.save(demoClass);

        // Update demo limit
        limitService.incrementDemoUsage(studentId);

        log.info("Demo class scheduled successfully: {}", saved.getId());

        // Publish event
        eventPublisher.publishDemoClassScheduled(saved);

        return toDto(saved);
    }

    @Transactional
    public DemoClassDto confirmDemoClass(String demoId, String meetingUrl,
                                         String meetingId, String meetingPassword) {
        log.info("Confirming demo class: {}", demoId);

        DemoClass demo = demoRepository.findById(demoId)
                .orElseThrow(() -> new IllegalArgumentException("Demo class not found: " + demoId));

        if (demo.getStatus() != DemoClass.DemoStatus.SCHEDULED) {
            throw new IllegalArgumentException("Only scheduled demo classes can be confirmed");
        }

        demo.setStatus(DemoClass.DemoStatus.CONFIRMED);
        demo.setMeetingUrl(meetingUrl);
        demo.setMeetingId(meetingId);
        demo.setMeetingPassword(meetingPassword);

        DemoClass updated = demoRepository.save(demo);
        log.info("Demo class confirmed: {}", demoId);

        return toDto(updated);
    }

    @Transactional
    public DemoClassDto startDemoClass(String demoId, String teacherId) {
        log.info("Starting demo class: {}", demoId);

        DemoClass demo = demoRepository.findById(demoId)
                .orElseThrow(() -> new IllegalArgumentException("Demo class not found: " + demoId));

        if (!demo.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this demo class");
        }

        if (demo.getStatus() != DemoClass.DemoStatus.CONFIRMED) {
            throw new IllegalArgumentException("Demo class is not confirmed");
        }

        demo.setStatus(DemoClass.DemoStatus.IN_PROGRESS);
        demo.setActualStartTime(LocalDateTime.now());

        DemoClass updated = demoRepository.save(demo);
        log.info("Demo class started: {}", demoId);

        return toDto(updated);
    }

    @Transactional
    public DemoClassDto completeDemoClass(String demoId, String teacherId,
                                          String teacherFeedback, Integer teacherRating) {
        log.info("Completing demo class: {}", demoId);

        DemoClass demo = demoRepository.findById(demoId)
                .orElseThrow(() -> new IllegalArgumentException("Demo class not found: " + demoId));

        if (!demo.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this demo class");
        }

        if (demo.getStatus() != DemoClass.DemoStatus.IN_PROGRESS) {
            throw new IllegalArgumentException("Demo class is not in progress");
        }

        demo.setStatus(DemoClass.DemoStatus.COMPLETED);
        demo.setActualEndTime(LocalDateTime.now());
        demo.setTeacherFeedback(teacherFeedback);
        demo.setTeacherRating(teacherRating);

        DemoClass updated = demoRepository.save(demo);
        log.info("Demo class completed: {}", demoId);

        // Publish event
        eventPublisher.publishDemoClassCompleted(updated);

        return toDto(updated);
    }

    @Transactional
    public DemoClassDto submitStudentFeedback(String demoId, String studentId,
                                              Integer rating) {
        DemoClass demo = demoRepository.findById(demoId)
                .orElseThrow(() -> new IllegalArgumentException("Demo class not found: " + demoId));

        if (!demo.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Unauthorized: Student does not own this demo class");
        }

        demo.setStudentRating(rating);
        DemoClass updated = demoRepository.save(demo);

        log.info("Student feedback submitted for demo: {}", demoId);
        return toDto(updated);
    }

    @Transactional
    public void cancelDemoClass(String demoId, String userId) {
        log.info("Cancelling demo class: {}", demoId);

        DemoClass demo = demoRepository.findById(demoId)
                .orElseThrow(() -> new IllegalArgumentException("Demo class not found: " + demoId));

        if (!demo.getStudentId().equals(userId) && !demo.getTeacherId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized to cancel this demo class");
        }

        if (demo.getStatus() == DemoClass.DemoStatus.COMPLETED) {
            throw new IllegalArgumentException("Cannot cancel completed demo class");
        }

        demo.setStatus(DemoClass.DemoStatus.CANCELLED);
        demoRepository.save(demo);

        log.info("Demo class cancelled: {}", demoId);
    }

    public DemoClassDto getDemoClass(String demoId) {
        DemoClass demo = demoRepository.findById(demoId)
                .orElseThrow(() -> new IllegalArgumentException("Demo class not found: " + demoId));
        return toDto(demo);
    }

    public List<DemoClassDto> getStudentDemos(String studentId) {
        return demoRepository.findByStudentId(studentId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<DemoClassDto> getTeacherDemos(String teacherId) {
        return demoRepository.findByTeacherId(teacherId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private DemoClassDto toDto(DemoClass demo) {
        return DemoClassDto.builder()
                .id(demo.getId())
                .studentId(demo.getStudentId())
                .studentName(demo.getStudentName())
                .studentEmail(demo.getStudentEmail())
                .teacherId(demo.getTeacherId())
                .teacherName(demo.getTeacherName())
                .courseId(demo.getCourseId())
                .courseName(demo.getCourseName())
                .status(demo.getStatus())
                .scheduledStartTime(demo.getScheduledStartTime())
                .scheduledEndTime(demo.getScheduledEndTime())
                .actualStartTime(demo.getActualStartTime())
                .actualEndTime(demo.getActualEndTime())
                .durationMinutes(demo.getDurationMinutes())
                .meetingUrl(demo.getMeetingUrl())
                .meetingId(demo.getMeetingId())
                .meetingPassword(demo.getMeetingPassword())
                .studentNotes(demo.getStudentNotes())
                .teacherFeedback(demo.getTeacherFeedback())
                .studentRating(demo.getStudentRating())
                .teacherRating(demo.getTeacherRating())
                .convertedToFullCourse(demo.getConvertedToFullCourse())
                .enrollmentId(demo.getEnrollmentId())
                .reminderSent(demo.getReminderSent())
                .createdAt(demo.getCreatedAt())
                .updatedAt(demo.getUpdatedAt())
                .build();
    }
}
