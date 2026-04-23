package com.tcon.learning_management_service.resource.controller;

import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.Grade;
import com.tcon.learning_management_service.course.entity.Subject;
import com.tcon.learning_management_service.course.entity.Topic;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import com.tcon.learning_management_service.course.repository.GradeRepository;
import com.tcon.learning_management_service.course.repository.SubjectRepository;
import com.tcon.learning_management_service.course.repository.TopicRepository;
import com.tcon.learning_management_service.resource.dto.ResourceDto;
import com.tcon.learning_management_service.resource.dto.ResourceUpdateRequest;
import com.tcon.learning_management_service.resource.dto.UpdateStatusRequest;
import com.tcon.learning_management_service.resource.service.ResourceService;
import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final ClassSessionRepository classSessionRepository;
    private final CourseRepository courseRepository;
    private final TopicRepository topicRepository;
    private final SubjectRepository subjectRepository;
    private final GradeRepository gradeRepository;

    // ─── ADMIN / TEACHER CREATE ───────────────────────────────────────────────

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_TEACHER')")
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ResourceDto> createResource(
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("gradeId") String gradeId,
            @RequestPart("subjectId") String subjectId,
            @RequestPart("topicId") String topicId,
            @RequestPart("files") MultipartFile[] files,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String uploader = (userId != null && !userId.isBlank())
                ? userId : "SYSTEM_ADMIN";

        ResourceDto dto = resourceService.createResource(
                title,
                description,
                gradeId,
                subjectId,
                topicId,
                files,
                uploader
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    // ─── ADMIN LIST ───────────────────────────────────────────────────────────

    /**
     * Admin list: all / only active / only inactive
     * GET /api/resources/admin?isActive=true|false
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/admin")
    public ResponseEntity<List<ResourceDto>> getAllResourcesForAdmin(
            @RequestParam(required = false) Boolean isActive) {
        return ResponseEntity.ok(resourceService.getAllResourcesForAdmin(isActive));
    }

    // ─── FILTERED LIST (student / parent / teacher) ───────────────────────────

    /**
     * Student/Parent/Teacher: filtered by grade/subject/topic, only active resources.
     */
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STUDENT', 'ROLE_PARENT', 'ROLE_TEACHER')")
    @GetMapping
    public ResponseEntity<List<ResourceDto>> getResources(
            @RequestParam(required = false) String gradeId,
            @RequestParam(required = false) String subjectId,
            @RequestParam(required = false) String topicId) {
        return ResponseEntity.ok(
                resourceService.getResourcesForFilters(gradeId, subjectId, topicId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STUDENT', 'ROLE_PARENT', 'ROLE_TEACHER')")
    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<ResourceDto>> getResourcesForStudent(@PathVariable String studentId) {
        return ResponseEntity.ok(resourceService.getResourcesForStudent(studentId));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STUDENT', 'ROLE_PARENT', 'ROLE_TEACHER')")
    @GetMapping("/student/{studentId}/course/{courseId}")
    public ResponseEntity<List<ResourceDto>> getResourcesForStudentCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return ResponseEntity.ok(resourceService.getResourcesForStudentCourse(studentId, courseId));
    }


    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STUDENT', 'ROLE_PARENT', 'ROLE_TEACHER')")
    @GetMapping("/{id}")
    public ResponseEntity<ResourceDto> getResourceById(@PathVariable String id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_STUDENT', 'ROLE_PARENT', 'ROLE_TEACHER')")
    @GetMapping("/topics/{topicId}")
    public ResponseEntity<List<ResourceDto>> getResourcesByTopic(
            @PathVariable String topicId) {
        return ResponseEntity.ok(resourceService.getResourcesByTopic(topicId));
    }

    // ─── ADMIN UPDATE ─────────────────────────────────────────────────────────

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ResourceDto> updateResource(
            @PathVariable String id,
            @RequestBody ResourceUpdateRequest request) {
        return ResponseEntity.ok(resourceService.updateResource(id, request));
    }

    // ─── ADMIN DELETE ─────────────────────────────────────────────────────────

    /**
     * HARD DELETE: permanently remove the resource.
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteResource(@PathVariable String id) {
        resourceService.hardDeleteResource(id);
        return ResponseEntity.ok(Map.of("message", "Resource deleted successfully"));
    }

    // ─── ADMIN STATUS TOGGLE ──────────────────────────────────────────────────

    /**
     * Toggle active/inactive status from admin.
     * PATCH /api/resources/{id}/status  { "isActive": true/false }
     */
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ResourceDto> updateResourceStatus(
            @PathVariable String id,
            @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.ok(
                resourceService.updateResourceStatus(id, request.getIsActive()));
    }

    // ─── TEACHER: UPLOAD FOR A SPECIFIC CLASS SESSION ─────────────────────────

    @PreAuthorize("hasAuthority('ROLE_TEACHER')")
    @PostMapping(
            path = "/teacher/session/{classSessionId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ResourceDto> uploadFromSession(
            @PathVariable String classSessionId,
            @RequestHeader("X-User-Id") String teacherId,
            @RequestPart("title") String title,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart("topicId") String topicId,
            @RequestPart("files") MultipartFile[] files
    ) {
        log.info("Teacher {} uploading resources for classSession {}",
                teacherId, classSessionId);

        String gradeId;
        String subjectId;

        // ── Step 1: Try to find ClassSession in LMS ───────────────────────────
        ClassSession session = classSessionRepository.findById(classSessionId)
                .orElse(null);

        if (session == null) {
            // ── Step 2: ClassSession not found (e.g. VideoSession classSessionId)
            // Derive grade/subject directly from the selected topicId
            log.warn("⚠️ ClassSession not found for id: {} — deriving grade/subject from topicId",
                    classSessionId);

            Topic topic = topicRepository.findById(topicId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Topic not found: " + topicId));

            Subject subject = subjectRepository.findById(topic.getSubjectId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Subject not found: " + topic.getSubjectId()));

            Grade grade = gradeRepository.findById(subject.getGradeId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Grade not found: " + subject.getGradeId()));

            gradeId   = grade.getId();
            subjectId = subject.getId();

            log.info("✅ Derived - Grade: {}, Subject: {}, Topic: {}",
                    grade.getName(), subject.getName(), topic.getName());

        } else {
            // ── Step 3: ClassSession found — validate teacher ownership ────────
            if (!session.getTeacherId().equals(teacherId)) {
                throw new IllegalArgumentException(
                        "Unauthorized: Teacher does not own this session");
            }

            // ── Step 4: Try to get grade/subject from Course ───────────────────
            if (session.getCourseId() != null && !session.getCourseId().isBlank()) {
                Course course = courseRepository.findById(session.getCourseId())
                        .orElse(null);

                if (course != null
                        && course.getGradeId() != null
                        && course.getSubjectId() != null) {
                    gradeId   = course.getGradeId();
                    subjectId = course.getSubjectId();
                    log.info("✅ Grade/Subject from course: {}", course.getId());
                } else {
                    // Course exists but has no grade/subject → fall back to topic
                    log.warn("⚠️ Course found but missing gradeId/subjectId — falling back to topicId");
                    Topic topic = topicRepository.findById(topicId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Topic not found: " + topicId));
                    Subject subject = subjectRepository.findById(topic.getSubjectId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Subject not found: " + topic.getSubjectId()));
                    gradeId   = subject.getGradeId();
                    subjectId = subject.getId();
                }
            } else {
                // ── Step 5: ONE_ON_ONE session (no courseId) → derive from topic
                log.info("ℹ️ ONE_ON_ONE session — deriving grade/subject from topicId");

                Topic topic = topicRepository.findById(topicId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Topic not found: " + topicId));

                Subject subject = subjectRepository.findById(topic.getSubjectId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Subject not found: " + topic.getSubjectId()));

                Grade grade = gradeRepository.findById(subject.getGradeId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Grade not found: " + subject.getGradeId()));

                gradeId   = grade.getId();
                subjectId = subject.getId();

                log.info("✅ Derived - Grade: {}, Subject: {}, Topic: {}",
                        grade.getName(), subject.getName(), topic.getName());
            }
        }

        ResourceDto resource = resourceService.createFromClassSession(
                title,
                description,
                gradeId,
                subjectId,
                topicId,
                files,
                teacherId
        );

        log.info("✅ Resource uploaded successfully for classSession: {}", classSessionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(resource);
    }

    @PreAuthorize("hasAnyAuthority('ROLE_PARENT', 'ROLE_ADMIN')")
    @GetMapping("/parent/student/{studentId}/course/{courseId}")
    public ResponseEntity<List<ResourceDto>> getResourcesForChildCourse(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return ResponseEntity.ok(
                resourceService.getResourcesForStudentCourse(studentId, courseId));
    }


}