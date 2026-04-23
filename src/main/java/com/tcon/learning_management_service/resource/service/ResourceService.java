package com.tcon.learning_management_service.resource.service;

import com.tcon.learning_management_service.course.entity.*;
import com.tcon.learning_management_service.course.repository.GradeRepository;
import com.tcon.learning_management_service.course.repository.SubjectRepository;
import com.tcon.learning_management_service.course.repository.TopicRepository;
import com.tcon.learning_management_service.resource.dto.FileUploadResponse;
import com.tcon.learning_management_service.resource.dto.ResourceDto;
import com.tcon.learning_management_service.resource.dto.ResourceFileDto;
import com.tcon.learning_management_service.resource.dto.ResourceUpdateRequest;
import com.tcon.learning_management_service.resource.entity.Resource;
import com.tcon.learning_management_service.resource.entity.ResourceFile;
import com.tcon.learning_management_service.resource.exception.ResourceNotFoundException;
import com.tcon.learning_management_service.resource.repository.ResourceRepository;
import com.tcon.learning_management_service.course.repository.CourseEnrollmentRepository;
import com.tcon.learning_management_service.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final GradeRepository gradeRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final ResourceStorageService resourceStorageService;
    private final CourseRepository courseRepository;
    private final CourseEnrollmentRepository enrollmentRepository;

    public ResourceDto createResource(String title,
                                      String description,
                                      String gradeId,
                                      String subjectId,
                                      String topicId,
                                      MultipartFile[] files,
                                      String adminUserId) {

        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new IllegalArgumentException("Grade not found: " + gradeId));

        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found: " + subjectId));

        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> new IllegalArgumentException("Topic not found: " + topicId));

        if (!subject.getGradeId().equals(grade.getId())) {
            throw new IllegalArgumentException("Selected subject does not belong to selected grade");
        }

        if (!topic.getSubjectId().equals(subject.getId())) {
            throw new IllegalArgumentException("Selected topic does not belong to selected subject");
        }

        if (files == null || files.length == 0) {
            throw new IllegalArgumentException("At least one PDF file is required");
        }

        List<ResourceFile> uploadedFiles = new ArrayList<>();

        for (MultipartFile file : files) {
            validatePdf(file);

            FileUploadResponse uploaded = resourceStorageService.uploadPdf(
                    file,
                    topicId
            );

            uploadedFiles.add(ResourceFile.builder()
                    .fileId(uploaded.getFileId())
                    .fileName(uploaded.getFileName())
                    .fileUrl(uploaded.getFileUrl())
                    .fileType("pdf")
                    .fileSizeBytes(uploaded.getFileSize())
                    .build());
        }

        Resource resource = Resource.builder()
                .title(title)
                .description(description)
                .gradeId(grade.getId())
                .gradeName(grade.getName())
                .subjectId(subject.getId())
                .subjectName(subject.getName())
                .topicId(topic.getId())
                .topicName(topic.getName())
                .files(uploadedFiles)
                .uploadedBy(adminUserId)
                .uploadedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        Resource saved = resourceRepository.save(resource);
        log.info("Resource created successfully with id={} for topicId={}", saved.getId(), topicId);
        return toDto(saved);
    }

    public List<ResourceDto> getAllActiveResources() {
        return resourceRepository.findByIsActiveTrueOrderByUploadedAtDesc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ResourceDto> getResourcesForFilters(String gradeId, String subjectId, String topicId) {
        List<Resource> resources;

        if (gradeId != null && subjectId != null && topicId != null) {
            resources = resourceRepository
                    .findByGradeIdAndSubjectIdAndTopicIdAndIsActiveTrueOrderByUploadedAtDesc(
                            gradeId, subjectId, topicId);
        } else if (gradeId != null && subjectId != null) {
            resources = resourceRepository
                    .findByGradeIdAndSubjectIdAndIsActiveTrueOrderByUploadedAtDesc(
                            gradeId, subjectId);
        } else if (gradeId != null) {
            resources = resourceRepository
                    .findByGradeIdAndIsActiveTrueOrderByUploadedAtDesc(gradeId);
        } else {
            resources = resourceRepository.findByIsActiveTrueOrderByUploadedAtDesc();
        }

        return resources.stream().map(this::toDto).collect(Collectors.toList());
    }

    public List<ResourceDto> getResourcesByTopic(String topicId) {
        return resourceRepository.findByTopicIdAndIsActiveTrueOrderByUploadedAtDesc(topicId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<ResourceDto> getAllResourcesForAdmin(Boolean isActive) {
        List<Resource> resources = isActive == null
                ? resourceRepository.findAll().stream()
                .sorted((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()))
                .toList()
                : resourceRepository.findByIsActiveOrderByUploadedAtDesc(isActive);

        return resources.stream().map(this::toDto).collect(Collectors.toList());
    }

    public ResourceDto getResourceById(String id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        return toDto(resource);
    }

    public ResourceDto updateResource(String id, ResourceUpdateRequest request) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            resource.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            resource.setDescription(request.getDescription());
        }

        if (request.getIsActive() != null) {
            resource.setIsActive(request.getIsActive());
        }

        resource.setUpdatedAt(LocalDateTime.now());
        return toDto(resourceRepository.save(resource));
    }

    /**
     * Soft delete: mark resource as inactive but keep it in DB.
     * (You can still call this from other flows if needed.)
     */
    public void softDeleteResource(String id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        resource.setIsActive(false);
        resource.setUpdatedAt(LocalDateTime.now());
        resourceRepository.save(resource);
        log.info("Resource soft deleted successfully, id={}", id);
    }

    /**
     * Hard delete: permanently remove the resource document.
     */
    public void hardDeleteResource(String id) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));

        // If you also want to delete underlying files from storage,
        // iterate resource.getFiles() and call resourceStorageService.delete(fileId) here.

        resourceRepository.delete(resource);
        log.info("Resource hard deleted successfully, id={}", id);
    }

    /**
     * Toggle active / inactive status (used by admin "Mark Active/Inactive").
     */
    public ResourceDto updateResourceStatus(String id, Boolean isActive) {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));

        resource.setIsActive(isActive);
        resource.setUpdatedAt(LocalDateTime.now());

        Resource saved = resourceRepository.save(resource);
        log.info("Resource status updated, id={}, isActive={}", id, isActive);
        return toDto(saved);
    }

    public List<ResourceDto> getResourcesForStudent(String studentId) {
        List<CourseEnrollment> enrollments = enrollmentRepository.findByStudentIdAndStatus(
                studentId,
                CourseEnrollment.EnrollmentStatus.ACTIVE
        );

        if (enrollments.isEmpty()) {
            return List.of();
        }

        List<String> courseIds = enrollments.stream()
                .map(CourseEnrollment::getCourseId)
                .distinct()
                .toList();

        List<Course> courses = courseRepository.findAllById(courseIds);

        List<String> allowedTopicIds = courses.stream()
                .filter(course -> course.getTopicIds() != null)
                .flatMap(course -> course.getTopicIds().stream())
                .distinct()
                .toList();

        if (allowedTopicIds.isEmpty()) {
            return List.of();
        }

        return resourceRepository.findByTopicIdInAndIsActiveTrueOrderByUploadedAtDesc(allowedTopicIds)
                .stream()
                .map(this::toDto)
                .toList();
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();

        boolean pdfByName = originalName.endsWith(".pdf");
        boolean pdfByContentType = contentType.contains("pdf");

        if (!pdfByName && !pdfByContentType) {
            throw new IllegalArgumentException("Only PDF files are allowed: " + file.getOriginalFilename());
        }
    }

    private ResourceDto toDto(Resource resource) {
        return ResourceDto.builder()
                .id(resource.getId())
                .title(resource.getTitle())
                .description(resource.getDescription())
                .gradeId(resource.getGradeId())
                .gradeName(resource.getGradeName())
                .subjectId(resource.getSubjectId())
                .subjectName(resource.getSubjectName())
                .topicId(resource.getTopicId())
                .topicName(resource.getTopicName())
                .files(resource.getFiles() == null ? List.of() : resource.getFiles().stream()
                        .map(file -> ResourceFileDto.builder()
                                .fileId(file.getFileId())
                                .fileName(file.getFileName())
                                .fileUrl(file.getFileUrl())
                                .fileType(file.getFileType())
                                .fileSizeBytes(file.getFileSizeBytes())
                                .build())
                        .collect(Collectors.toList()))
                .uploadedBy(resource.getUploadedBy())
                .uploadedAt(resource.getUploadedAt())
                .updatedAt(resource.getUpdatedAt())
                .isActive(resource.getIsActive())
                .build();
    }

    public ResourceDto createFromClassSession(
            String title,
            String description,
            String gradeId,
            String subjectId,
            String topicId,
            MultipartFile[] files,
            String createdBy
    ) {
        return createResource(
                title,
                description,
                gradeId,
                subjectId,
                topicId,
                files,
                createdBy
        );
    }

    public List<ResourceDto> getResourcesForStudentCourse(String studentId, String courseId) {
        CourseEnrollment enrollment = enrollmentRepository
                .findByCourseIdAndStudentId(courseId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student is not enrolled in this course"));

        if (enrollment.getStatus() != CourseEnrollment.EnrollmentStatus.ACTIVE) {
            return List.of();
        }

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        List<String> topicIds = course.getTopicIds() == null ? List.of() : course.getTopicIds();

        if (topicIds.isEmpty()) {
            return List.of();
        }

        return resourceRepository.findByTopicIdInAndIsActiveTrueOrderByUploadedAtDesc(topicIds)
                .stream()
                .map(this::toDto)
                .toList();
    }
}