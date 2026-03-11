package com.tcon.learning_management_service.course.controller;

import com.tcon.learning_management_service.course.dto.GradeDto;
import com.tcon.learning_management_service.course.dto.SubjectDto;
import com.tcon.learning_management_service.course.dto.TopicDto;
import com.tcon.learning_management_service.course.service.GradeService;
import com.tcon.learning_management_service.course.service.SubjectService;
import com.tcon.learning_management_service.course.service.TopicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MasterDataController {

    private final GradeService gradeService;
    private final SubjectService subjectService;
    private final TopicService topicService;

    // =========================
    //          GRADES
    // =========================

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/api/grades")
    public ResponseEntity<GradeDto> createGrade(@Valid @RequestBody GradeDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(gradeService.create(dto));
    }

    @GetMapping("/api/grades")
    public ResponseEntity<List<GradeDto>> getAllGrades() {
        return ResponseEntity.ok(gradeService.getAll());
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/api/grades/{id}")
    public ResponseEntity<GradeDto> updateGrade(
            @PathVariable String id,
            @RequestBody GradeDto dto) {
        return ResponseEntity.ok(gradeService.update(id, dto));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/api/grades/{id}")
    public ResponseEntity<Map<String, String>> deleteGrade(@PathVariable String id) {
        gradeService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Grade deleted successfully"));
    }

    // =========================
    //         SUBJECTS
    // =========================

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/api/subjects")
    public ResponseEntity<SubjectDto> createSubject(@Valid @RequestBody SubjectDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subjectService.create(dto));
    }

    // Frontend calls this when admin selects a grade to get its subjects
    @GetMapping("/api/grades/{gradeId}/subjects")
    public ResponseEntity<List<SubjectDto>> getSubjectsByGrade(@PathVariable String gradeId) {
        return ResponseEntity.ok(subjectService.getByGrade(gradeId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/api/subjects/{id}")
    public ResponseEntity<SubjectDto> updateSubject(
            @PathVariable String id,
            @RequestBody SubjectDto dto) {
        return ResponseEntity.ok(subjectService.update(id, dto));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/api/subjects/{id}")
    public ResponseEntity<Map<String, String>> deleteSubject(@PathVariable String id) {
        subjectService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Subject deleted successfully"));
    }

    // =========================
    //          TOPICS
    // =========================

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping("/api/topics")
    public ResponseEntity<TopicDto> createTopic(@Valid @RequestBody TopicDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(topicService.create(dto));
    }

    // Frontend calls this when admin selects a subject to get its topics
    @GetMapping("/api/subjects/{subjectId}/topics")
    public ResponseEntity<List<TopicDto>> getTopicsBySubject(@PathVariable String subjectId) {
        return ResponseEntity.ok(topicService.getBySubject(subjectId));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/api/topics/{id}")
    public ResponseEntity<TopicDto> updateTopic(
            @PathVariable String id,
            @RequestBody TopicDto dto) {
        return ResponseEntity.ok(topicService.update(id, dto));
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/api/topics/{id}")
    public ResponseEntity<Map<String, String>> deleteTopic(@PathVariable String id) {
        topicService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Topic deleted successfully"));
    }
}
