package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.dto.AssignmentCreateRequest;
import com.tcon.learning_management_service.assignment.dto.AssignStudentsRequest;
import com.tcon.learning_management_service.assignment.dto.StartAssignmentRequest;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.entity.Submission;
import com.tcon.learning_management_service.assignment.entity.Question; // ✅ ADDED
import com.tcon.learning_management_service.assignment.service.AssignmentService;
import com.tcon.learning_management_service.assignment.service.SubmissionService;
import com.tcon.learning_management_service.assignment.service.QuestionService; // ✅ ADDED

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final QuestionService questionService; // ✅ ADDED

    @PostMapping
    public Assignment createAssignment(@RequestBody AssignmentCreateRequest request) {
        return assignmentService.createAssignment(request);
    }

    @PostMapping("/{assignmentId}/assign")
    public Assignment assignStudents(@PathVariable String assignmentId,
                                     @RequestBody AssignStudentsRequest request) {
        return assignmentService.assignStudents(assignmentId, request);
    }

    @GetMapping("/{assignmentId}")
    public Assignment getAssignment(@PathVariable String assignmentId) {
        return assignmentService.getAssignment(assignmentId);
    }

    @PostMapping("/{assignmentId}/start")
    public Submission startAssignment(@PathVariable String assignmentId,
                                      @RequestBody StartAssignmentRequest request) {
        return submissionService.startAssignment(assignmentId, request.getStudentId());
    }

    @PostMapping("/{assignmentId}/activity")
    public String trackStudentActivity(@PathVariable String assignmentId,
                                       @RequestParam String studentId) {
        submissionService.trackActivity(assignmentId, studentId);
        return "Activity recorded";
    }

    @GetMapping("/student/{studentId}")
    public List<Assignment> getStudentAssignments(@PathVariable String studentId) {
        return assignmentService.getAssignmentsForStudent(studentId);
    }

    @GetMapping("/teacher/{teacherId}")
    public List<Assignment> getTeacherAssignments(@PathVariable String teacherId) {
        return assignmentService.getAssignmentsForTeacher(teacherId);
    }

    @GetMapping("/{assignmentId}/results")
    public List<Submission> getResults(@PathVariable String assignmentId) {
        return submissionService.getResults(assignmentId);
    }

    /**
     * ✅ UPDATED: Parent can view their child's assignment results (SECURED)
     */
    @GetMapping("/parent/student/{studentId}/results")
    public List<Submission> getStudentResultsForParent(
            @PathVariable String studentId,
            @RequestHeader("X-User-Id") String parentId,
            @RequestHeader("X-User-Role") String role) {
        return submissionService.getResultsForParent(parentId, studentId, role);
    }

    /**
     * 🔥 NEW: Get Questions for Assignment
     */
    @GetMapping("/{assignmentId}/questions")
    public List<Question> getQuestionsByAssignment(@PathVariable String assignmentId) {

        Assignment assignment = assignmentService.getAssignment(assignmentId);

        return questionService.getQuestionsByIds(assignment.getQuestionIds());
    }
}