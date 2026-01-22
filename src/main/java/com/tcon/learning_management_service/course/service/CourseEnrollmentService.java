package com.tcon.learning_management_service.course.service;

import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.CourseEnrollment;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import com.tcon.learning_management_service.course.repository.CourseEnrollmentRepository;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import com.tcon.learning_management_service.event.CourseEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseEnrollmentService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseEventPublisher eventPublisher;

    @Transactional
    public CourseEnrollment enrollStudent(String courseId, String studentId, String studentName,
                                          String studentEmail, String paymentId, BigDecimal amountPaid) {
        log.info("Enrolling student {} in course {}", studentId, courseId);

        // Get course
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // Validate course status
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException("Course is not available for enrollment");
        }

        // Check if already enrolled
        if (enrollmentRepository.existsByCourseIdAndStudentId(courseId, studentId)) {
            throw new IllegalArgumentException("Student is already enrolled in this course");
        }

        // Check capacity
        if (course.getMaxStudents() != null &&
                course.getCurrentEnrollments() >= course.getMaxStudents()) {
            throw new IllegalArgumentException("Course is full");
        }

        // Create enrollment
        CourseEnrollment enrollment = CourseEnrollment.builder()
                .courseId(courseId)
                .studentId(studentId)
                .studentName(studentName)
                .studentEmail(studentEmail)
                .status(CourseEnrollment.EnrollmentStatus.ACTIVE)
                .enrolledAt(LocalDateTime.now())
                .amountPaid(amountPaid)
                .paymentId(paymentId)
                .sessionsAttended(0)
                .totalSessions(course.getTotalSessions())
                .progressPercentage(0.0)
                .build();

        CourseEnrollment saved = enrollmentRepository.save(enrollment);

        // Update course enrollment count
        course.setCurrentEnrollments(course.getCurrentEnrollments() + 1);
        courseRepository.save(course);

        log.info("Student enrolled successfully: {}", saved.getId());

        // Publish event
        eventPublisher.publishStudentEnrolled(courseId, studentId, studentName);

        return saved;
    }

    public List<CourseEnrollment> getStudentEnrollments(String studentId) {
        return enrollmentRepository.findByStudentId(studentId);
    }

    public List<CourseEnrollment> getCourseEnrollments(String courseId) {
        return enrollmentRepository.findByCourseId(courseId);
    }

    public CourseEnrollment getEnrollment(String courseId, String studentId) {
        return enrollmentRepository.findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));
    }

    @Transactional
    public void cancelEnrollment(String enrollmentId, String studentId) {
        log.info("Cancelling enrollment: {}", enrollmentId);

        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        if (!enrollment.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("Unauthorized: Student does not own this enrollment");
        }

        if (enrollment.getStatus() != CourseEnrollment.EnrollmentStatus.ACTIVE) {
            throw new IllegalArgumentException("Only active enrollments can be cancelled");
        }

        enrollment.setStatus(CourseEnrollment.EnrollmentStatus.CANCELLED);
        enrollment.setCancelledAt(LocalDateTime.now());
        enrollmentRepository.save(enrollment);

        // Update course enrollment count
        Course course = courseRepository.findById(enrollment.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        course.setCurrentEnrollments(Math.max(0, course.getCurrentEnrollments() - 1));
        courseRepository.save(course);

        log.info("Enrollment cancelled successfully: {}", enrollmentId);

        // Publish event
        eventPublisher.publishStudentUnenrolled(enrollment.getCourseId(), studentId);
    }

    @Transactional
    public void updateProgress(String enrollmentId, Integer sessionsAttended) {
        CourseEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new IllegalArgumentException("Enrollment not found"));

        enrollment.setSessionsAttended(sessionsAttended);

        if (enrollment.getTotalSessions() > 0) {
            double progress = (sessionsAttended * 100.0) / enrollment.getTotalSessions();
            enrollment.setProgressPercentage(Math.min(progress, 100.0));
        }

        // Mark as completed if all sessions attended
        if (sessionsAttended >= enrollment.getTotalSessions()) {
            enrollment.setStatus(CourseEnrollment.EnrollmentStatus.COMPLETED);
            enrollment.setCompletedAt(LocalDateTime.now());
        }

        enrollmentRepository.save(enrollment);
        log.info("Enrollment progress updated: {} - {}%", enrollmentId, enrollment.getProgressPercentage());
    }
}
