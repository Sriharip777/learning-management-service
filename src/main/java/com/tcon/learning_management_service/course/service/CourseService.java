package com.tcon.learning_management_service.course.service;

import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.course.client.UserServiceClient;
import com.tcon.learning_management_service.course.dto.AssignedTeacherDto;
import com.tcon.learning_management_service.course.dto.AvailableTeacherDto;
import com.tcon.learning_management_service.course.dto.CourseCreateRequest;
import com.tcon.learning_management_service.course.dto.CourseDto;
import com.tcon.learning_management_service.course.dto.CourseSessionDto;
import com.tcon.learning_management_service.course.dto.CourseUpdateRequest;
import com.tcon.learning_management_service.course.dto.EligibleTeacherRequest;
import com.tcon.learning_management_service.course.dto.TeacherResponseDto;
import com.tcon.learning_management_service.course.dto.UserResponseDto;
import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.CourseEnrollment;
import com.tcon.learning_management_service.course.entity.CourseSession;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import com.tcon.learning_management_service.course.entity.Grade;
import com.tcon.learning_management_service.course.entity.Subject;
import com.tcon.learning_management_service.course.entity.Topic;
import com.tcon.learning_management_service.course.repository.CourseEnrollmentRepository;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import com.tcon.learning_management_service.course.repository.GradeRepository;
import com.tcon.learning_management_service.course.repository.SubjectRepository;
import com.tcon.learning_management_service.course.repository.TopicRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseService {

    private static final String DEFAULT_CURRENCY = "INR";
    private static final String DEFAULT_TEACHER_NAME = "Expert Instructor";

    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;
    private final CourseEventPublisher eventPublisher;
    private final UserServiceClient userServiceClient;
    private final ClassSessionService classSessionService;
    private final BookingRepository bookingRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    // =========================
    //        ADMIN ONLY
    // =========================

    public List<Course> getCoursesByTeacherId(String teacherId) {
        if (!hasText(teacherId)) {
            return List.of();
        }

        List<Course> legacyCourses = courseRepository.findByTeacherId(teacherId);
        List<Course> allCourses = courseRepository.findAll();

        List<Course> multiTeacherCourses = allCourses.stream()
                .filter(Objects::nonNull)
                .filter(course -> containsTeacher(course, teacherId))
                .toList();

        return mergeDistinctCourses(legacyCourses, multiTeacherCourses);
    }

    @Transactional
    public CourseDto createCourseByAdmin(String adminId, CourseCreateRequest request) {
        log.info("Admin {} creating course {}", adminId, request.getTitle());

        validateDatesAndCapacity(request);

        Grade grade = gradeRepository.findById(request.getGradeId())
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + request.getGradeId()));
        if (Boolean.FALSE.equals(grade.getIsActive())) {
            throw new IllegalStateException("Grade is inactive: " + grade.getName());
        }

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + request.getSubjectId()));
        if (!Objects.equals(subject.getGradeId(), grade.getId())) {
            throw new IllegalArgumentException("Subject does not belong to selected grade");
        }
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            throw new IllegalStateException("Subject is inactive: " + subject.getName());
        }

        if (CollectionUtils.isEmpty(request.getTopicIds())) {
            throw new IllegalArgumentException("At least one topic must be selected");
        }

        List<Topic> topics = topicRepository.findAllById(request.getTopicIds());
        if (topics.size() != request.getTopicIds().size()) {
            throw new IllegalArgumentException("Some topics were not found");
        }
        if (topics.stream().anyMatch(t -> !Objects.equals(t.getSubjectId(), subject.getId()))) {
            throw new IllegalArgumentException("One or more topics do not belong to selected subject");
        }
        if (topics.stream().anyMatch(t -> Boolean.FALSE.equals(t.getIsActive()))) {
            throw new IllegalStateException("One or more topics are inactive");
        }

        List<CourseSession> sessionEntities = null;
        if (!CollectionUtils.isEmpty(request.getSessions())) {
            sessionEntities = request.getSessions().stream()
                    .map(this::toSessionEntityFromScheduleRequest)
                    .toList();
        }

        Course course = Course.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .gradeId(grade.getId())
                .subjectId(subject.getId())
                .topicIds(cleanStringList(request.getTopicIds()))
                .status(CourseStatus.DRAFT)
                .pricePerSession(request.getPricePerSession())
                .currency(hasText(request.getCurrency()) ? request.getCurrency().trim() : DEFAULT_CURRENCY)
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
                .teacherId(null)
                .teacherIds(new ArrayList<>())
                .build();

        Course saved = courseRepository.save(course);
        log.info("Course created successfully with ID: {}", saved.getId());

        autoCreateClassSessions(saved.getId(), null, request.getSessions());

        eventPublisher.publishCourseCreated(saved);

        return toDtoWithMasterData(saved, grade, subject, topics);
    }

    @Transactional
    public CourseDto updateCourseByAdmin(String courseId, String adminId, CourseUpdateRequest request) {
        log.info("Admin {} updating course {}", adminId, courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        String newGradeId = hasText(request.getGradeId()) ? request.getGradeId().trim() : course.getGradeId();
        String newSubjectId = hasText(request.getSubjectId()) ? request.getSubjectId().trim() : course.getSubjectId();
        List<String> newTopicIds = request.getTopicIds() != null
                ? cleanStringList(request.getTopicIds())
                : defaultList(course.getTopicIds());

        Grade grade = gradeRepository.findById(newGradeId)
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + newGradeId));
        if (Boolean.FALSE.equals(grade.getIsActive())) {
            throw new IllegalStateException("Grade is inactive: " + grade.getName());
        }

        Subject subject = subjectRepository.findById(newSubjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + newSubjectId));
        if (!Objects.equals(subject.getGradeId(), grade.getId())) {
            throw new IllegalArgumentException("Subject does not belong to selected grade");
        }
        if (Boolean.FALSE.equals(subject.getIsActive())) {
            throw new IllegalStateException("Subject is inactive: " + subject.getName());
        }

        if (newTopicIds.isEmpty()) {
            throw new IllegalArgumentException("At least one topic must be selected");
        }

        List<Topic> topics = topicRepository.findAllById(newTopicIds);
        if (topics.size() != newTopicIds.size()) {
            throw new IllegalArgumentException("Some topics were not found");
        }
        if (topics.stream().anyMatch(t -> !Objects.equals(t.getSubjectId(), subject.getId()))) {
            throw new IllegalArgumentException("One or more topics do not belong to selected subject");
        }
        if (topics.stream().anyMatch(t -> Boolean.FALSE.equals(t.getIsActive()))) {
            throw new IllegalStateException("One or more topics are inactive");
        }

        if (request.getTitle() != null) course.setTitle(request.getTitle());
        if (request.getDescription() != null) course.setDescription(request.getDescription());
        if (request.getStatus() != null) course.setStatus(request.getStatus());
        if (request.getPricePerSession() != null) course.setPricePerSession(request.getPricePerSession());
        if (request.getMinStudents() != null) course.setMinStudents(request.getMinStudents());
        if (request.getMaxStudents() != null) course.setMaxStudents(request.getMaxStudents());
        if (request.getSchedule() != null) course.setSchedule(request.getSchedule());
        if (request.getStartDate() != null) course.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) course.setEndDate(request.getEndDate());
        if (request.getPrerequisites() != null) course.setPrerequisites(defaultList(request.getPrerequisites()));
        if (request.getLearningOutcomes() != null) course.setLearningOutcomes(defaultList(request.getLearningOutcomes()));
        if (request.getThumbnailUrl() != null) course.setThumbnailUrl(request.getThumbnailUrl());
        if (request.getMaterialUrls() != null) course.setMaterialUrls(defaultList(request.getMaterialUrls()));
        if (request.getIsDemoAvailable() != null) course.setIsDemoAvailable(request.getIsDemoAvailable());
        if (request.getDemoSessionDuration() != null) course.setDemoSessionDuration(request.getDemoSessionDuration());

        course.setGradeId(newGradeId);
        course.setSubjectId(newSubjectId);
        course.setTopicIds(newTopicIds);
        normalizeTeacherAssignments(course);
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

        normalizeTeacherAssignments(course);
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

        normalizeTeacherAssignments(course);

        Grade grade = course.getGradeId() != null ? gradeRepository.findById(course.getGradeId()).orElse(null) : null;
        Subject subject = course.getSubjectId() != null ? subjectRepository.findById(course.getSubjectId()).orElse(null) : null;
        List<Topic> topics = course.getTopicIds() != null ? topicRepository.findAllById(course.getTopicIds()) : List.of();

        return toDtoWithMasterData(course, grade, subject, topics);
    }

    public List<CourseDto> getTeacherCourses(String teacherId) {
        return getCoursesByTeacherId(teacherId).stream()
                .map(c -> {
                    normalizeTeacherAssignments(c);
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
                    normalizeTeacherAssignments(c);
                    Grade grade = c.getGradeId() != null ? gradeRepository.findById(c.getGradeId()).orElse(null) : null;
                    Subject subject = c.getSubjectId() != null ? subjectRepository.findById(c.getSubjectId()).orElse(null) : null;
                    List<Topic> topics = c.getTopicIds() != null ? topicRepository.findAllById(c.getTopicIds()) : List.of();
                    return toDtoWithMasterData(c, grade, subject, topics);
                })
                .collect(Collectors.toList());
    }

    public List<CourseDto> getStudentCourses(String studentId) {
        List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                CourseEnrollment.EnrollmentStatus.ACTIVE
        );

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<String> courseIds = enrollments.stream()
                .map(CourseEnrollment::getCourseId)
                .filter(this::hasText)
                .distinct()
                .toList();

        return courseRepository.findAllById(courseIds).stream()
                .map(course -> {
                    normalizeTeacherAssignments(course);

                    Grade grade = course.getGradeId() != null
                            ? gradeRepository.findById(course.getGradeId()).orElse(null)
                            : null;
                    Subject subject = course.getSubjectId() != null
                            ? subjectRepository.findById(course.getSubjectId()).orElse(null)
                            : null;
                    List<Topic> topics = course.getTopicIds() != null
                            ? topicRepository.findAllById(course.getTopicIds())
                            : List.of();

                    return toDtoWithMasterData(course, grade, subject, topics);
                })
                .toList();
    }

    // =========================
    //  COMMUNICATION / MAPPING
    // =========================

    public boolean canUsersCommunicate(String user1, String user2) {
        if (!hasText(user1) || !hasText(user2)) {
            return false;
        }

        List<CourseEnrollment> user1Enrollments = enrollmentRepository.findByStudentId(user1);
        for (CourseEnrollment enrollment : user1Enrollments) {
            if (enrollment.getStatus() == CourseEnrollment.EnrollmentStatus.ACTIVE) {
                Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                if (course != null && containsTeacher(course, user2)) {
                    return true;
                }
            }
        }

        List<CourseEnrollment> user2Enrollments = enrollmentRepository.findByStudentId(user2);
        for (CourseEnrollment enrollment : user2Enrollments) {
            if (enrollment.getStatus() == CourseEnrollment.EnrollmentStatus.ACTIVE) {
                Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
                if (course != null && containsTeacher(course, user1)) {
                    return true;
                }
            }
        }

        return false;
    }

    public List<String> getTeachersForStudent(String studentId) {
        List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(
                studentId, CourseEnrollment.EnrollmentStatus.ACTIVE);

        List<String> courseIds = enrollments.stream()
                .map(CourseEnrollment::getCourseId)
                .filter(this::hasText)
                .distinct()
                .toList();

        Set<String> teacherIds = new LinkedHashSet<>();

        for (Course course : courseRepository.findAllById(courseIds)) {
            teacherIds.addAll(getTeacherIdsFromCourse(course));
        }

        return teacherIds.stream().toList();
    }

    public List<String> getStudentsForTeacher(String teacherId) {
        if (!hasText(teacherId)) return List.of();

        Set<String> studentIds = new LinkedHashSet<>();

        List<Course> courses = getCoursesByTeacherId(teacherId);
        List<String> courseIds = courses.stream()
                .map(Course::getId)
                .filter(this::hasText)
                .distinct()
                .toList();

        for (String courseId : courseIds) {
            List<CourseEnrollment> enrollments = enrollmentRepository.findByCourseIdAndStatus(
                    courseId, CourseEnrollment.EnrollmentStatus.ACTIVE);
            studentIds.addAll(enrollments.stream()
                    .map(CourseEnrollment::getStudentId)
                    .filter(this::hasText)
                    .toList());
        }

        List<Booking> bookings = bookingRepository.findByTeacherId(teacherId);
        studentIds.addAll(bookings.stream()
                .filter(Objects::nonNull)
                .filter(booking -> booking.getStatus() == BookingStatus.CONFIRMED
                        || booking.getStatus() == BookingStatus.PENDING
                        || booking.getStatus() == BookingStatus.PENDING_PAYMENT)
                .map(Booking::getStudentId)
                .filter(this::hasText)
                .toList());

        return studentIds.stream().toList();
    }

    // =========================
    //       DTO HELPERS
    // =========================

    private CourseDto toDtoWithMasterData(Course course,
                                          Grade grade,
                                          Subject subject,
                                          List<Topic> topics) {

        normalizeTeacherAssignments(course);

        List<String> topicNames = topics == null ? List.of()
                : topics.stream().filter(Objects::nonNull).map(Topic::getName).toList();

        List<CourseSessionDto> sessionDtos = null;
        if (course.getSessions() != null) {
            sessionDtos = course.getSessions().stream()
                    .map(this::toSessionDto)
                    .toList();
        }

        CourseDto dto = CourseDto.builder()
                .id(course.getId())
                .teacherId(course.getTeacherId())
                .teacherIds(defaultList(course.getTeacherIds()))
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

    private void enrichWithTeacherInfo(CourseDto courseDto, String teacherUserId) {
        if (!hasText(teacherUserId)) {
            return;
        }

        try {
            UserResponseDto user = userServiceClient.getUserById(teacherUserId);
            if (user != null) {
                String name = buildDisplayName(user);
                courseDto.setTeacherName(name);
                courseDto.setTeacherEmail(user.getEmail());
                courseDto.setTeacherProfilePicture(user.getProfilePicture());
                log.debug("Teacher name resolved: {}", name);
            }
        } catch (Exception e) {
            log.warn("Could not fetch user for teacherUserId {}: {}", teacherUserId, e.getMessage());
            courseDto.setTeacherName(DEFAULT_TEACHER_NAME);
        }

        try {
            TeacherResponseDto teacher = userServiceClient.getTeacherByUserId(teacherUserId);
            if (teacher != null) {
                courseDto.setTeacherBio(teacher.getBio());
                courseDto.setTeacherSubjects(defaultList(teacher.getSubjects()));
                courseDto.setTeacherLanguages(defaultList(teacher.getLanguages()));
                courseDto.setTeacherYearsOfExperience(teacher.getYearsOfExperience());
                courseDto.setTeacherQualifications(teacher.getQualifications());
                courseDto.setTeacherHourlyRate(teacher.getHourlyRate());
                courseDto.setTeacherRating(teacher.getAverageRating());
                courseDto.setTeacherTotalReviews(teacher.getTotalReviews());
                courseDto.setTeacherExpertise(defaultList(teacher.getSubjects()));
                courseDto.setTeacherTimezone(teacher.getTimezone());
                courseDto.setTeacherIsAvailable(teacher.getIsAvailable());
                courseDto.setTeacherVerificationStatus(teacher.getVerificationStatus());
            }
        } catch (Exception e) {
            log.warn("Teacher profile not found for userId {}: {}", teacherUserId, e.getMessage());
        }

        try {
            Integer totalStudents = calculateTeacherTotalStudents(teacherUserId);
            courseDto.setTeacherTotalStudents(totalStudents);
        } catch (Exception e) {
            log.warn("Could not calculate total students for {}", teacherUserId);
        }
    }

    private Integer calculateTeacherTotalStudents(String teacherUserId) {
        try {
            List<Course> teacherCourses = getCoursesByTeacherId(teacherUserId);
            return teacherCourses.stream()
                    .filter(Objects::nonNull)
                    .filter(c -> c.getCurrentEnrollments() != null)
                    .mapToInt(Course::getCurrentEnrollments)
                    .sum();
        } catch (Exception e) {
            log.error("Error calculating total students for teacher userId: {}", teacherUserId, e);
            return 0;
        }
    }

    public List<AvailableTeacherDto> getAvailableTeachersForCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        normalizeTeacherAssignments(course);

        List<TeacherResponseDto> eligibleTeachers = getEligibleTeachersForCourse(courseId);

        if (eligibleTeachers.isEmpty()) {
            log.warn("No eligible teachers found for courseId={}, gradeId={}, subjectId={}",
                    courseId, course.getGradeId(), course.getSubjectId());
            return List.of();
        }

        return eligibleTeachers.stream()
                .filter(Objects::nonNull)
                .map(teacher -> {
                    String displayName = DEFAULT_TEACHER_NAME;
                    String avatar = null;

                    try {
                        UserResponseDto user = userServiceClient.getUserById(teacher.getUserId());
                        if (user != null) {
                            displayName = buildDisplayName(user);
                            avatar = user.getProfilePicture();
                        }
                    } catch (Exception e) {
                        log.warn("Could not fetch user details for eligible teacher {}: {}",
                                teacher.getUserId(), e.getMessage());
                    }

                    return AvailableTeacherDto.builder()
                            .id(teacher.getUserId())
                            .name(displayName)
                            .avatar(avatar)
                            .hourlyRate(teacher.getHourlyRate())
                            .currency(hasText(course.getCurrency()) ? course.getCurrency() : DEFAULT_CURRENCY)
                            .rating(teacher.getAverageRating())
                            .subjects(defaultList(teacher.getSubjects()))
                            .build();
                })
                .toList();
    }

    private void validateTeacherEligibilityForCourse(Course course, String teacherUserId) {
        if (!hasText(teacherUserId)) {
            throw new IllegalArgumentException("Teacher userId is required");
        }

        normalizeTeacherAssignments(course);

        Grade grade = course.getGradeId() != null ? gradeRepository.findById(course.getGradeId()).orElse(null) : null;
        Subject subject = course.getSubjectId() != null ? subjectRepository.findById(course.getSubjectId()).orElse(null) : null;

        List<TeacherResponseDto> eligibleTeachers = getEligibleTeachersForCourse(course.getId());

        boolean isEligible = eligibleTeachers.stream()
                .filter(Objects::nonNull)
                .map(TeacherResponseDto::getUserId)
                .filter(this::hasText)
                .anyMatch(teacherUserId::equals);

        if (!isEligible) {
            String gradeLabel = grade != null ? grade.getName() : course.getGradeId();
            String subjectLabel = subject != null ? subject.getName() : course.getSubjectId();

            throw new IllegalArgumentException(
                    "Teacher not eligible for this course. Required: " + gradeLabel + " - " + subjectLabel
            );
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

    public List<TeacherResponseDto> getEligibleTeachersForCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        normalizeTeacherAssignments(course);

        if (!hasText(course.getGradeId())) {
            log.warn("Eligible teacher lookup skipped: missing gradeId for courseId={}", courseId);
            return List.of();
        }

        if (!hasText(course.getSubjectId())) {
            log.warn("Eligible teacher lookup skipped: missing subjectId for courseId={}", courseId);
            return List.of();
        }

        EligibleTeacherRequest req = EligibleTeacherRequest.builder()
                .gradeId(course.getGradeId())
                .subjectId(course.getSubjectId())
                .topicIds(defaultList(course.getTopicIds()))
                .build();

        log.info("Eligible teacher lookup started for courseId={}, gradeId={}, subjectId={}, topicIds={}",
                courseId, req.getGradeId(), req.getSubjectId(), req.getTopicIds());

        try {
            List<TeacherResponseDto> teachers = userServiceClient.getEligibleTeachersForCourse(req);

            log.info("Eligible teacher lookup success for courseId={}, count={}, teacherUserIds={}",
                    courseId,
                    teachers != null ? teachers.size() : 0,
                    teachers != null
                            ? teachers.stream().filter(Objects::nonNull).map(TeacherResponseDto::getUserId).toList()
                            : List.of());

            return teachers != null ? teachers : List.of();

        } catch (FeignException.NotFound ex) {
            log.warn("Eligible teacher lookup 404 for courseId={}, gradeId={}, subjectId={}, body={}",
                    courseId, req.getGradeId(), req.getSubjectId(), ex.contentUTF8());
            return List.of();

        } catch (FeignException ex) {
            log.error("Eligible teacher lookup failed for courseId={}, gradeId={}, subjectId={}, status={}, body={}",
                    courseId, req.getGradeId(), req.getSubjectId(), ex.status(), ex.contentUTF8(), ex);
            return List.of();

        } catch (Exception ex) {
            log.error("Unexpected eligible teacher lookup failure for courseId={}, gradeId={}, subjectId={}: {}",
                    courseId, req.getGradeId(), req.getSubjectId(), ex.getMessage(), ex);
            return List.of();
        }
    }

    // ========= ASSIGN / UNASSIGN MULTIPLE TEACHERS =========

    @Transactional
    public CourseDto assignTeacherToCourse(String courseId, String teacherUserId, String adminId) {
        log.info("Admin {} assigning teacherUserId: {} to courseId: {}",
                adminId, teacherUserId, courseId);

        if (!hasText(teacherUserId)) {
            throw new IllegalArgumentException("Teacher userId is required");
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        normalizeTeacherAssignments(course);
        validateTeacherEligibilityForCourse(course, teacherUserId);

        if (course.getTeacherIds() == null) {
            course.setTeacherIds(new ArrayList<>());
        }

        if (!course.getTeacherIds().contains(teacherUserId)) {
            course.getTeacherIds().add(teacherUserId);
        }

        normalizeTeacherAssignments(course);

        course.setUpdatedBy(adminId);
        course.setUpdatedAt(LocalDateTime.now());

        Course saved = courseRepository.save(course);
        log.info("Course {} now has teacherId={}, teacherIds={}", courseId, saved.getTeacherId(), saved.getTeacherIds());

        Grade grade = saved.getGradeId() != null
                ? gradeRepository.findById(saved.getGradeId()).orElse(null) : null;
        Subject subject = saved.getSubjectId() != null
                ? subjectRepository.findById(saved.getSubjectId()).orElse(null) : null;
        List<Topic> topics = saved.getTopicIds() != null
                ? topicRepository.findAllById(saved.getTopicIds()) : List.of();

        return toDtoWithMasterData(saved, grade, subject, topics);
    }

    @Transactional
    public CourseDto unassignTeacherFromCourse(String courseId, String teacherUserId, String adminId) {
        log.info("Admin {} unassigning teacherUserId: {} from courseId: {}",
                adminId, teacherUserId, courseId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        normalizeTeacherAssignments(course);

        if (course.getTeacherIds() == null || course.getTeacherIds().isEmpty()) {
            Grade grade = course.getGradeId() != null ? gradeRepository.findById(course.getGradeId()).orElse(null) : null;
            Subject subject = course.getSubjectId() != null ? subjectRepository.findById(course.getSubjectId()).orElse(null) : null;
            List<Topic> topics = course.getTopicIds() != null ? topicRepository.findAllById(course.getTopicIds()) : List.of();
            return toDtoWithMasterData(course, grade, subject, topics);
        }

        course.setTeacherIds(course.getTeacherIds().stream()
                .filter(this::hasText)
                .filter(id -> !id.equals(teacherUserId))
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new)));

        normalizeTeacherAssignments(course);

        course.setUpdatedBy(adminId);
        course.setUpdatedAt(LocalDateTime.now());

        Course saved = courseRepository.save(course);
        log.info("Course {} now has teacherId={}, teacherIds={} after unassign",
                courseId, saved.getTeacherId(), saved.getTeacherIds());

        Grade grade = saved.getGradeId() != null
                ? gradeRepository.findById(saved.getGradeId()).orElse(null) : null;
        Subject subject = saved.getSubjectId() != null
                ? subjectRepository.findById(saved.getSubjectId()).orElse(null) : null;
        List<Topic> topics = saved.getTopicIds() != null
                ? topicRepository.findAllById(saved.getTopicIds()) : List.of();

        return toDtoWithMasterData(saved, grade, subject, topics);
    }

    public List<AssignedTeacherDto> getAssignedTeachersForCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        normalizeTeacherAssignments(course);
        List<String> teacherUserIds = defaultList(course.getTeacherIds());

        if (teacherUserIds.isEmpty()) {
            log.info("No assigned teachers found for courseId {}", courseId);
            return List.of();
        }

        return teacherUserIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(id -> !id.isBlank())
                .distinct()
                .map(this::buildAssignedTeacherSafely)
                .filter(Objects::nonNull)
                .toList();
    }

    private AssignedTeacherDto buildAssignedTeacherSafely(String userId) {
        try {
            UserResponseDto user = null;
            TeacherResponseDto teacher = null;

            try {
                user = userServiceClient.getUserById(userId);
            } catch (FeignException ex) {
                log.warn("Failed to fetch user details for assigned teacher userId {}: status={}, message={}",
                        userId, ex.status(), ex.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to fetch user details for assigned teacher userId {}: {}",
                        userId, ex.getMessage());
            }

            try {
                teacher = userServiceClient.getTeacherByUserId(userId);
            } catch (FeignException ex) {
                log.warn("Failed to fetch teacher profile for assigned teacher userId {}: status={}, message={}",
                        userId, ex.status(), ex.getMessage());
            } catch (Exception ex) {
                log.warn("Failed to fetch teacher profile for assigned teacher userId {}: {}",
                        userId, ex.getMessage());
            }

            if (user == null && teacher == null) {
                log.warn("Skipping assigned teacher userId {} because both user and teacher profile are unavailable", userId);
                return null;
            }

            AssignedTeacherDto dto = new AssignedTeacherDto();
            dto.setUserId(userId);
            dto.setFirstName(user != null ? user.getFirstName() : null);
            dto.setLastName(user != null ? user.getLastName() : null);
            dto.setAvatar(user != null ? user.getProfilePicture() : null);
            dto.setAverageRating(teacher != null ? teacher.getAverageRating() : null);
            dto.setTotalReviews(teacher != null ? teacher.getTotalReviews() : null);
            dto.setSubjects(teacher != null && teacher.getSubjects() != null ? teacher.getSubjects() : List.of());
            dto.setLanguages(teacher != null && teacher.getLanguages() != null ? teacher.getLanguages() : List.of());
            dto.setHourlyRate(teacher != null ? teacher.getHourlyRate() : null);
            dto.setCurrency(DEFAULT_CURRENCY);

            return dto;
        } catch (Exception ex) {
            log.error("Unexpected error while building assigned teacher for userId {}: {}", userId, ex.getMessage(), ex);
            return null;
        }
    }

    private String buildDisplayName(UserResponseDto user) {
        if (user == null) return DEFAULT_TEACHER_NAME;

        String firstName = user.getFirstName();
        String lastName = user.getLastName();

        if (hasText(firstName) && hasText(lastName)) {
            return (firstName + " " + lastName).trim();
        }
        if (hasText(firstName)) return firstName;
        if (hasText(lastName)) return lastName;
        if (hasText(user.getName())) return user.getName();
        if (hasText(user.getEmail())) return user.getEmail().split("@")[0];

        return DEFAULT_TEACHER_NAME;
    }

    // =========================
    //       UTIL HELPERS
    // =========================

    private <T> List<T> defaultList(List<T> list) {
        return list != null ? list : new ArrayList<>();
    }

    private List<String> cleanStringList(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private void validateDatesAndCapacity(CourseCreateRequest request) {
        if (request.getStartDate() == null) {
            throw new IllegalArgumentException("Start date is required");
        }
        if (request.getEndDate() == null) {
            throw new IllegalArgumentException("End date is required");
        }
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

                classSessionService.scheduleSession(teacherId, sessionRequest);

                log.info("Session {} created for course {}", sessionNumber, courseId);
                sessionNumber++;
            } catch (Exception e) {
                log.error("Failed to create session for course {}: {}", courseId, e.getMessage(), e);
            }
        }
    }

    private void normalizeTeacherAssignments(Course course) {
        if (course == null) {
            return;
        }

        List<String> normalizedTeacherIds = new ArrayList<>();

        if (course.getTeacherIds() != null) {
            normalizedTeacherIds.addAll(course.getTeacherIds().stream()
                    .filter(this::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList());
        }

        if (hasText(course.getTeacherId()) && !normalizedTeacherIds.contains(course.getTeacherId().trim())) {
            normalizedTeacherIds.add(0, course.getTeacherId().trim());
        }

        course.setTeacherIds(normalizedTeacherIds);

        if (normalizedTeacherIds.isEmpty()) {
            course.setTeacherId(null);
        } else if (!hasText(course.getTeacherId()) || !normalizedTeacherIds.contains(course.getTeacherId().trim())) {
            course.setTeacherId(normalizedTeacherIds.get(0));
        }
    }

    private boolean containsTeacher(Course course, String teacherId) {
        if (course == null || !hasText(teacherId)) {
            return false;
        }

        normalizeTeacherAssignments(course);

        return course.getTeacherIds() != null && course.getTeacherIds().contains(teacherId);
    }

    private List<String> getTeacherIdsFromCourse(Course course) {
        if (course == null) {
            return List.of();
        }

        normalizeTeacherAssignments(course);
        return defaultList(course.getTeacherIds()).stream()
                .filter(this::hasText)
                .distinct()
                .toList();
    }

    private List<Course> mergeDistinctCourses(List<Course> first, List<Course> second) {
        Set<String> seen = new LinkedHashSet<>();
        List<Course> result = new ArrayList<>();

        for (Course course : defaultList(first)) {
            if (course != null && hasText(course.getId()) && seen.add(course.getId())) {
                result.add(course);
            }
        }

        for (Course course : defaultList(second)) {
            if (course != null && hasText(course.getId()) && seen.add(course.getId())) {
                result.add(course);
            }
        }

        return result;
    }
}