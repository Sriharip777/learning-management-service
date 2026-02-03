package com.tcon.learning_management_service.course.service;


import com.tcon.learning_management_service.course.dto.CourseCreateRequest;
import com.tcon.learning_management_service.course.dto.CourseDto;
import com.tcon.learning_management_service.course.dto.CourseUpdateRequest;
import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import com.tcon.learning_management_service.course.repository.CourseEnrollmentRepository;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import com.tcon.learning_management_service.event.CourseEventPublisher;
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
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseEventPublisher eventPublisher;

    @Transactional
    public CourseDto createCourse(String teacherId, CourseCreateRequest request) {
        log.info("Creating course for teacher: {}", teacherId);

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (request.getMaxStudents() != null && request.getMinStudents() != null) {
            if (request.getMaxStudents() < request.getMinStudents()) {
                throw new IllegalArgumentException("Max students cannot be less than min students");
            }
        }

        Course course = Course.builder()
                .teacherId(teacherId)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .tags(request.getTags() != null ? request.getTags() : new ArrayList<>())
                .status(CourseStatus.DRAFT)
                .pricePerSession(request.getPricePerSession())
                .currency(request.getCurrency())
                .minStudents(request.getMinStudents())
                .maxStudents(request.getMaxStudents())
                .currentEnrollments(0)
                .gradeLevel(request.getGradeLevel())
                .difficulty(request.getDifficulty())
                .schedule(request.getSchedule())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalSessions(request.getTotalSessions())
                .completedSessions(0)
                .prerequisites(request.getPrerequisites() != null ? request.getPrerequisites() : new ArrayList<>())
                .learningOutcomes(request.getLearningOutcomes() != null ? request.getLearningOutcomes() : new ArrayList<>())
                .thumbnailUrl(request.getThumbnailUrl())
                .materialUrls(request.getMaterialUrls() != null ? request.getMaterialUrls() : new ArrayList<>())
                .isDemoAvailable(request.getIsDemoAvailable())
                .demoSessionDuration(request.getDemoSessionDuration())
                .rating(0.0)
                .totalReviews(0)
                .createdBy(teacherId)
                .build();

        Course saved = courseRepository.save(course);
        log.info("Course created successfully with ID: {}", saved.getId());

        // Publish event
        eventPublisher.publishCourseCreated(saved);

        return toDto(saved);
    }

    @Transactional
    public CourseDto updateCourse(String courseId, String teacherId, CourseUpdateRequest request) {
        log.info("Updating course: {} by teacher: {}", courseId, teacherId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // Verify teacher ownership
        if (!course.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this course");
        }

        // Update fields if provided
        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            course.setStatus(request.getStatus());
        }
        if (request.getPricePerSession() != null) {
            course.setPricePerSession(request.getPricePerSession());
        }
        if (request.getMinStudents() != null) {
            course.setMinStudents(request.getMinStudents());
        }
        if (request.getMaxStudents() != null) {
            course.setMaxStudents(request.getMaxStudents());
        }
        if (request.getSchedule() != null) {
            course.setSchedule(request.getSchedule());
        }
        if (request.getStartDate() != null) {
            course.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            course.setEndDate(request.getEndDate());
        }
        if (request.getPrerequisites() != null) {
            course.setPrerequisites(request.getPrerequisites());
        }
        if (request.getLearningOutcomes() != null) {
            course.setLearningOutcomes(request.getLearningOutcomes());
        }
        if (request.getThumbnailUrl() != null) {
            course.setThumbnailUrl(request.getThumbnailUrl());
        }
        if (request.getMaterialUrls() != null) {
            course.setMaterialUrls(request.getMaterialUrls());
        }
        if (request.getIsDemoAvailable() != null) {
            course.setIsDemoAvailable(request.getIsDemoAvailable());
        }
        if (request.getDemoSessionDuration() != null) {
            course.setDemoSessionDuration(request.getDemoSessionDuration());
        }

        course.setUpdatedBy(teacherId);

        Course updated = courseRepository.save(course);
        log.info("Course updated successfully: {}", courseId);

        // Publish event
        eventPublisher.publishCourseUpdated(updated);

        return toDto(updated);
    }

    /**
     * Check if two users can communicate based on course enrollments
     * Returns true if:
     * - user1 is student enrolled in user2 (teacher)'s course, OR
     * - user2 is student enrolled in user1 (teacher)'s course
     */
    public boolean canUsersCommunicate(String user1, String user2) {
        log.debug("Checking communication permission between {} and {}", user1, user2);

        // Check if user1 is student enrolled in any course taught by user2
        List<CourseEnrollment> user1Enrollments = enrollmentRepository.findByStudentId(user1);
        for (CourseEnrollment enrollment : user1Enrollments) {
            if (enrollment.getStatus() == CourseEnrollment.EnrollmentStatus.ACTIVE) {
                Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                if (course != null && course.getTeacherId().equals(user2)) {
                    log.debug("User {} is enrolled in course {} taught by {}", user1, course.getId(), user2);
                    return true;
                }
            }
        }

        // Check reverse: if user2 is student enrolled in any course taught by user1
        List<CourseEnrollment> user2Enrollments = enrollmentRepository.findByStudentId(user2);
        for (CourseEnrollment enrollment : user2Enrollments) {
            if (enrollment.getStatus() == CourseEnrollment.EnrollmentStatus.ACTIVE) {
                Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                if (course != null && course.getTeacherId().equals(user1)) {
                    log.debug("User {} is enrolled in course {} taught by {}", user2, course.getId(), user1);
                    return true;
                }
            }
        }

        log.debug("No enrollment relationship found between {} and {}", user1, user2);
        return false;
    }


    public CourseDto getCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));
        return toDto(course);
    }

    public List<CourseDto> getTeacherCourses(String teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CourseDto> getPublishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED, null).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void publishCourse(String courseId, String teacherId) {
        log.info("Publishing course: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (!course.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this course");
        }

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft courses can be published");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        courseRepository.save(course);

        log.info("Course published successfully: {}", courseId);
        eventPublisher.publishCoursePublished(course);
    }

    @Transactional
    public void deleteCourse(String courseId, String teacherId) {
        log.info("Deleting course: {}", courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (!course.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this course");
        }

        // Check if course has enrollments
        Long enrollmentCount = enrollmentRepository.countByCourseIdAndStatus(
                courseId,
                com.tcon.learning_management_service.course.entity.CourseEnrollment.EnrollmentStatus.ACTIVE
        );

        if (enrollmentCount > 0) {
            throw new IllegalArgumentException("Cannot delete course with active enrollments");
        }

        course.setStatus(CourseStatus.DELETED);
        courseRepository.save(course);

        log.info("Course deleted successfully: {}", courseId);
        eventPublisher.publishCourseDeleted(courseId);
    }

    private CourseDto toDto(Course course) {
        return CourseDto.builder()
                .id(course.getId())
                .teacherId(course.getTeacherId())
                .title(course.getTitle())
                .description(course.getDescription())
                .category(course.getCategory())
                .tags(course.getTags())
                .status(course.getStatus())
                .pricePerSession(course.getPricePerSession())
                .currency(course.getCurrency())
                .minStudents(course.getMinStudents())
                .maxStudents(course.getMaxStudents())
                .currentEnrollments(course.getCurrentEnrollments())
                .gradeLevel(course.getGradeLevel())
                .difficulty(course.getDifficulty())
                .schedule(course.getSchedule())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .totalSessions(course.getTotalSessions())
                .completedSessions(course.getCompletedSessions())
                .prerequisites(course.getPrerequisites())
                .learningOutcomes(course.getLearningOutcomes())
                .thumbnailUrl(course.getThumbnailUrl())
                .materialUrls(course.getMaterialUrls())
                .isDemoAvailable(course.getIsDemoAvailable())
                .demoSessionDuration(course.getDemoSessionDuration())
                .rating(course.getRating())
                .totalReviews(course.getTotalReviews())
                .createdAt(course.getCreatedAt())
                .updatedAt(course.getUpdatedAt())
                .build();
    }
}
