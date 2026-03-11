package com.tcon.learning_management_service.course.service;

import com.tcon.learning_management_service.course.client.UserServiceClient;
import com.tcon.learning_management_service.course.dto.*;
import com.tcon.learning_management_service.course.entity.*;
import com.tcon.learning_management_service.course.repository.*;
import com.tcon.learning_management_service.event.CourseEventPublisher;
import com.tcon.learning_management_service.session.dto.SessionScheduleRequest;
import com.tcon.learning_management_service.session.service.ClassSessionService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
    private final UserServiceClient userServiceClient;
    private final ClassSessionService classSessionService;

    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    // =========================
    //        ADMIN ONLY
    // =========================

    @Transactional
    public CourseDto createCourseByAdmin(String adminId, CourseCreateRequest request) {
        log.info("Admin {} creating course {}", adminId, request.getTitle());

        validateDatesAndCapacity(request);

        // 1) Validate grade
        Grade grade = gradeRepository.findById(request.getGradeId())
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + request.getGradeId()));
        if (Boolean.FALSE.equals(grade.getIsActive())) {
            throw new IllegalStateException("Grade is inactive: " + grade.getName());
        }

        // 2) Validate subject belongs to grade
        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + request.getSubjectId()));
        if (!subject.getGradeId().equals(grade.getId())) {
            throw new IllegalArgumentException("Subject does not belong to selected grade");
        }
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            throw new IllegalStateException("Subject is inactive: " + subject.getName());
        }

        // 3) Validate topics
        if (CollectionUtils.isEmpty(request.getTopicIds())) {
            throw new IllegalArgumentException("At least one topic must be selected");
        }

        List<Topic> topics = topicRepository.findAllById(request.getTopicIds());
        if (topics.size() != request.getTopicIds().size()) {
            throw new IllegalArgumentException("Some topics were not found");
        }
        if (topics.stream().anyMatch(t -> !t.getSubjectId().equals(subject.getId()))) {
            throw new IllegalArgumentException("One or more topics do not belong to selected subject");
        }
        if (topics.stream().anyMatch(t -> Boolean.FALSE.equals(t.getIsActive()))) {
            throw new IllegalStateException("One or more topics are inactive");
        }

        // 4) Map embedded sessions into Course.sessions
        List<CourseSession> sessionEntities = null;
        if (!CollectionUtils.isEmpty(request.getSessions())) {
            sessionEntities = request.getSessions().stream()
                    .map(this::toSessionEntityFromScheduleRequest)
                    .toList();
        }

        // 5) Build Course
        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .gradeId(grade.getId())
                .subjectId(subject.getId())
                .topicIds(request.getTopicIds())
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
                .prerequisites(defaultList(request.getPrerequisites()))
                .learningOutcomes(defaultList(request.getLearningOutcomes()))
                .sessions(sessionEntities)
                .thumbnailUrl(request.getThumbnailUrl())
                .materialUrls(defaultList(request.getMaterialUrls()))
                .isDemoAvailable(request.getIsDemoAvailable())
                .demoSessionDuration(request.getDemoSessionDuration())
                .rating(0.0)
                .totalReviews(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .createdBy(adminId)
                .updatedBy(adminId)
                .build();

        Course saved = courseRepository.save(course);
        log.info("Course created successfully with ID: {}", saved.getId());

        // 6) Optionally create class sessions in session service
        autoCreateClassSessions(saved.getId(), null, request.getSessions());

        // 7) Publish event
        eventPublisher.publishCourseCreated(saved);

        return toDtoWithMasterData(saved, grade, subject, topics);
    }

    @Transactional
    public CourseDto updateCourseByAdmin(String courseId, String adminId, CourseUpdateRequest request) {
        log.info("Admin {} updating course {}", adminId, courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        // 1) Determine new mapping IDs (fall back to existing if null)
        String newGradeId = request.getGradeId() != null ? request.getGradeId() : course.getGradeId();
        String newSubjectId = request.getSubjectId() != null ? request.getSubjectId() : course.getSubjectId();
        List<String> newTopicIds = request.getTopicIds() != null ? request.getTopicIds() : course.getTopicIds();

        // 2) Validate master data
        Grade grade = gradeRepository.findById(newGradeId)
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + newGradeId));
        if (Boolean.FALSE.equals(grade.getIsActive())) {
            throw new IllegalStateException("Grade is inactive: " + grade.getName());
        }

        Subject subject = subjectRepository.findById(newSubjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + newSubjectId));
        if (!subject.getGradeId().equals(grade.getId())) {
            throw new IllegalArgumentException("Subject does not belong to selected grade");
        }
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            throw new IllegalStateException("Subject is inactive: " + subject.getName());
        }

        if (newTopicIds == null || newTopicIds.isEmpty()) {
            throw new IllegalArgumentException("At least one topic must be selected");
        }

        List<Topic> topics = topicRepository.findAllById(newTopicIds);
        if (topics.size() != newTopicIds.size()) {
            throw new IllegalArgumentException("Some topics were not found");
        }
        if (topics.stream().anyMatch(t -> !t.getSubjectId().equals(subject.getId()))) {
            throw new IllegalArgumentException("One or more topics do not belong to selected subject");
        }
        if (topics.stream().anyMatch(t -> Boolean.FALSE.equals(t.getIsActive()))) {
            throw new IllegalStateException("One or more topics are inactive");
        }

        // 3) Simple field updates
        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getStatus() != null) course.setStatus(request.getStatus());
        if (request.getPricePerSession() != null) course.setPricePerSession(request.getPricePerSession());
        if (request.getMinStudents() != null) course.setMinStudents(request.getMinStudents());
        if (request.getMaxStudents() != null) course.setMaxStudents(request.getMaxStudents());
        if (request.getSchedule() != null) course.setSchedule(request.getSchedule());
        if (request.getStartDate() != null) course.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) course.setEndDate(request.getEndDate());
        if (request.getPrerequisites() != null) course.setPrerequisites(request.getPrerequisites());
        if (request.getLearningOutcomes() != null) course.setLearningOutcomes(request.getLearningOutcomes());
        if (request.getThumbnailUrl() != null) course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getMaterialUrls() != null) course.setMaterialUrls(request.getMaterialUrls());
        if (request.getIsDemoAvailable() != null) course.setIsDemoAvailable(request.getIsDemoAvailable());
        if (request.getDemoSessionDuration() != null) course.setDemoSessionDuration(request.getDemoSessionDuration());

        // 4) Apply new mapping
        course.setGradeId(newGradeId);
        course.setSubjectId(newSubjectId);
        course.setTopicIds(newTopicIds);
        course.setUpdatedBy(adminId);
        course.setUpdatedAt(LocalDateTime.now());

        Course updated = courseRepository.save(course);
        log.info("Course updated successfully: {}", courseId);

        eventPublisher.publishCourseUpdated(updated);

        return toDtoWithMasterData(updated, grade, subject, topics);
    }

    @Transactional
    public void publishCourseByAdmin(String courseId, String adminId) {
        log.info("Admin {} publishing course {}", adminId, courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (course.getStatus() != CourseStatus.DRAFT) {
            throw new IllegalArgumentException("Only draft courses can be published");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setUpdatedBy(adminId);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        log.info("Course published successfully: {}", courseId);
        eventPublisher.publishCoursePublished(course);
    }

    @Transactional
    public void unpublishCourseByAdmin(String courseId, String adminId) {
        log.info("Admin {} unpublishing course {}", adminId, courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException("Only published courses can be unpublished");
        }

        Long activeEnrollments = enrollmentRepository.countByCourseIdAndStatus(
                courseId, CourseEnrollment.EnrollmentStatus.ACTIVE);

        if (activeEnrollments > 0) {
            throw new IllegalArgumentException("Cannot unpublish course with active enrollments");
        }

        course.setStatus(CourseStatus.DRAFT);
        course.setUpdatedBy(adminId);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        log.info("Course unpublished successfully: {}", courseId);
    }

    @Transactional
    public void deleteCourseByAdmin(String courseId, String adminId) {
        log.info("Admin {} deleting course {}", adminId, courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        Long activeEnrollments = enrollmentRepository.countByCourseIdAndStatus(
                courseId, CourseEnrollment.EnrollmentStatus.ACTIVE);

        if (activeEnrollments > 0) {
            throw new IllegalArgumentException("Cannot delete course with active enrollments");
        }

        course.setStatus(CourseStatus.DELETED);
        course.setUpdatedBy(adminId);
        course.setUpdatedAt(LocalDateTime.now());
        courseRepository.save(course);

        log.info("Course deleted (soft) successfully: {}", courseId);
        eventPublisher.publishCourseDeleted(courseId);
    }

    // =========================
    //         READ APIs
    // =========================

    public CourseDto getCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        Grade grade = null;
        Subject subject = null;
        List<Topic> topics = new ArrayList<>();

        if (course.getGradeId() != null) {
            grade = gradeRepository.findById(course.getGradeId()).orElse(null);
        }
        if (course.getSubjectId() != null) {
            subject = subjectRepository.findById(course.getSubjectId()).orElse(null);
        }
        if (course.getTopicIds() != null && !course.getTopicIds().isEmpty()) {
            topics = topicRepository.findAllById(course.getTopicIds());
        }

        return toDtoWithMasterData(course, grade, subject, topics);
    }

    public List<CourseDto> getTeacherCourses(String teacherId) {
        return courseRepository.findByTeacherId(teacherId).stream()
                .map(c -> {
                    Grade grade = c.getGradeId() != null ? gradeRepository.findById(c.getGradeId()).orElse(null) : null;
                    Subject subject = c.getSubjectId() != null ? subjectRepository.findById(c.getSubjectId()).orElse(null) : null;
                    List<Topic> topics = c.getTopicIds() != null ? topicRepository.findAllById(c.getTopicIds()) : List.of();
                    return toDtoWithMasterData(c, grade, subject, topics);
                })
                .collect(Collectors.toList());
    }

    public List<CourseDto> getPublishedCourses() {
        return courseRepository.findByStatus(CourseStatus.PUBLISHED, null).stream()
                .map(c -> {
                    Grade grade = c.getGradeId() != null ? gradeRepository.findById(c.getGradeId()).orElse(null) : null;
                    Subject subject = c.getSubjectId() != null ? subjectRepository.findById(c.getSubjectId()).orElse(null) : null;
                    List<Topic> topics = c.getTopicIds() != null ? topicRepository.findAllById(c.getTopicIds()) : List.of();
                    return toDtoWithMasterData(c, grade, subject, topics);
                })
                .collect(Collectors.toList());
    }

    // =========================
    //  COMMUNICATION / MAPPING
    // =========================

    public boolean canUsersCommunicate(String user1, String user2) {
        log.debug("Checking communication permission between {} and {}", user1, user2);

        List<CourseEnrollment> user1Enrollments = enrollmentRepository.findByStudentId(user1);
        for (CourseEnrollment enrollment : user1Enrollments) {
            if (enrollment.getStatus() == CourseEnrollment.EnrollmentStatus.ACTIVE) {
                Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                if (course != null && user2.equals(course.getTeacherId())) {
                    return true;
                }
            }
        }

        List<CourseEnrollment> user2Enrollments = enrollmentRepository.findByStudentId(user2);
        for (CourseEnrollment enrollment : user2Enrollments) {
            if (enrollment.getStatus() == CourseEnrollment.EnrollmentStatus.ACTIVE) {
                Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                if (course != null && user1.equals(course.getTeacherId())) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<String> getTeachersForStudent(String studentId) {
        log.info("Finding teachers for student: {}", studentId);

        List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(
                studentId, CourseEnrollment.EnrollmentStatus.ACTIVE);

        List<String> courseIds = enrollments.stream()
                .map(CourseEnrollment::getCourseId)
                .distinct()
                .toList();

        List<String> teacherIds = courseRepository.findAllById(courseIds).stream()
                .map(Course::getTeacherId)
                .distinct()
                .toList();

        log.info("Found {} teachers for student {}", teacherIds.size(), studentId);
        return teacherIds;
    }

    public List<String> getStudentsForTeacher(String teacherId) {
        log.info("Finding students for teacher: {}", teacherId);

        List<Course> courses = courseRepository.findByTeacherId(teacherId);
        List<String> courseIds = courses.stream().map(Course::getId).toList();

        List<String> studentIds = new ArrayList<>();
        for (String courseId : courseIds) {
            List<CourseEnrollment> enrollments = enrollmentRepository.findByCourseIdAndStatus(
                    courseId, CourseEnrollment.EnrollmentStatus.ACTIVE);
            studentIds.addAll(enrollments.stream()
                    .map(CourseEnrollment::getStudentId)
                    .toList());
        }

        return studentIds.stream().distinct().toList();
    }

    // =========================
    //       DTO HELPERS
    // =========================

    private CourseDto toDtoWithMasterData(Course course,
                                          Grade grade,
                                          Subject subject,
                                          List<Topic> topics) {

        List<String> topicNames = topics == null ? List.of() :
                topics.stream().map(Topic::getName).toList();

        List<CourseSessionDto> sessionDtos = null;
        if (course.getSessions() != null) {
            sessionDtos = course.getSessions().stream()
                    .map(this::toSessionDto)
                    .toList();
        }

        CourseDto dto = CourseDto.builder()
                .id(course.getId())
                .teacherId(course.getTeacherId())
                .title(course.getTitle())
                .description(course.getDescription())
                .gradeId(course.getGradeId())
                .subjectId(course.getSubjectId())
                .topicIds(course.getTopicIds())
                .gradeName(grade != null ? grade.getName() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .topicNames(topicNames)
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
                .sessions(sessionDtos)
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


        enrichWithTeacherInfo(dto, course.getTeacherId());
        return dto;
    }

    private void enrichWithTeacherInfo(CourseDto courseDto, String teacherId) {
        if (teacherId == null) {
            return;
        }
        try {
            log.debug("Fetching teacher information for teacherId: {}", teacherId);
            TeacherResponseDto teacher = userServiceClient.getTeacherById(teacherId);

            if (teacher != null) {
                courseDto.setTeacherBio(teacher.getBio());
                courseDto.setTeacherSubjects(teacher.getSubjects());
                courseDto.setTeacherLanguages(teacher.getLanguages());
                courseDto.setTeacherYearsOfExperience(teacher.getYearsOfExperience());
                courseDto.setTeacherQualifications(teacher.getQualifications());
                courseDto.setTeacherHourlyRate(teacher.getHourlyRate());
                courseDto.setTeacherRating(teacher.getAverageRating());
                courseDto.setTeacherTotalReviews(teacher.getTotalReviews());
                courseDto.setTeacherExpertise(teacher.getSubjects());
                courseDto.setTeacherTimezone(teacher.getTimezone());
                courseDto.setTeacherIsAvailable(teacher.getIsAvailable());
                courseDto.setTeacherVerificationStatus(teacher.getVerificationStatus());

                if (teacher.getUserId() != null) {
                    UserResponseDto user = userServiceClient.getUserById(teacher.getUserId());
                    if (user != null) {
                        courseDto.setTeacherName(user.getName());
                        courseDto.setTeacherEmail(user.getEmail());
                        courseDto.setTeacherProfilePicture(user.getProfilePicture());
                    }
                }

                Integer totalStudents = calculateTeacherTotalStudents(teacherId);
                courseDto.setTeacherTotalStudents(totalStudents);
            }
        } catch (FeignException.NotFound e) {
            log.warn("Teacher not found for teacherId: {}", teacherId);
            courseDto.setTeacherName("Expert Instructor");
        } catch (Exception e) {
            log.error("Error fetching teacher information for teacherId: {}", teacherId, e);
            courseDto.setTeacherName("Expert Instructor");
        }
    }

    private Integer calculateTeacherTotalStudents(String teacherId) {
        try {
            List<Course> teacherCourses = courseRepository.findByTeacherId(teacherId);
            return teacherCourses.stream()
                    .filter(c -> c.getCurrentEnrollments() != null)
                    .mapToInt(Course::getCurrentEnrollments)
                    .sum();
        } catch (Exception e) {
            log.error("Error calculating total students for teacherId: {}", teacherId, e);
            return 0;
        }
    }

    // =========================
    //      SESSION HELPERS
    // =========================

    private CourseSessionDto toSessionDto(CourseSession session) {
        if (session == null) return null;
        return CourseSessionDto.builder()
                .title(session.getTitle())
                .description(session.getDescription())
                .topics(session.getTopics())
                .scheduledStartTime(session.getScheduledStartTime())
                .durationMinutes(session.getDurationMinutes())
                .build();
    }

    private CourseSession toSessionEntityFromScheduleRequest(SessionScheduleRequest ssr) {
        if (ssr == null) return null;
        return CourseSession.builder()
                .title(ssr.getTitle())
                .description(ssr.getDescription())
                .topics(ssr.getTopics())
                .scheduledStartTime(ssr.getScheduledStartTime())
                .durationMinutes(ssr.getDurationMinutes())
                .build();
    }

    // =========================
    //       UTIL HELPERS
    // =========================

    private <T> List<T> defaultList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }

    private void validateDatesAndCapacity(CourseCreateRequest request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }
        if (request.getMaxStudents() != null && request.getMinStudents() != null
                && request.getMaxStudents() < request.getMinStudents()) {
            throw new IllegalArgumentException("Max students cannot be less than min students");
        }
    }

    private void autoCreateClassSessions(String courseId,
                                         String teacherId,
                                         List<SessionScheduleRequest> sessionRequests) {
        if (sessionRequests == null || sessionRequests.isEmpty()) {
            return;
        }

        log.info("Creating {} class sessions for course {}", sessionRequests.size(), courseId);

        int sessionNumber = 1;
        for (SessionScheduleRequest sessionRequest : sessionRequests) {
            try {
                sessionRequest.setCourseId(courseId);

                if (sessionRequest.getTitle() != null && !sessionRequest.getTitle().contains("Session")) {
                    sessionRequest.setTitle("Session " + sessionNumber + " - " + sessionRequest.getTitle());
                }

                // teacherId may be null if admin creates course without specific teacher
                classSessionService.scheduleSession(teacherId, sessionRequest);

                log.info("Session {} created for course {}", sessionNumber, courseId);
                sessionNumber++;
            } catch (Exception e) {
                log.error("Failed to create session for course {}: {}", courseId, e.getMessage(), e);
            }
        }
    }
}
