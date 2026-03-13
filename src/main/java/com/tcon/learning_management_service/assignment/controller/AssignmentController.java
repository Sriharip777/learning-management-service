package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.dto.AssignmentCreateRequest;
import com.tcon.learning_management_service.assignment.dto.AssignStudentsRequest;
import com.tcon.learning_management_service.assignment.dto.StartAssignmentRequest;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.entity.Submission;
import com.tcon.learning_management_service.assignment.service.AssignmentService;
import com.tcon.learning_management_service.assignment.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    @PostMapping
    public Assignment createAssignment(@RequestBody AssignmentCreateRequest request) {
        return assignmentService.createAssignment(request);
    }

    @PostMapping("/{assignmentId}/assign")
    public Assignment assignStudents(@PathVariable String assignmentId,
                                     @RequestBody AssignStudentsRequest request) {
        return assignmentService.assignStudents(assignmentId, request);
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
     * Parent can view their child's assignment results
     */
    @GetMapping("/parent/student/{studentId}/results")
    public List<Submission> getStudentResultsForParent(@PathVariable String studentId) {
        return submissionService.getResultsByStudent(studentId);
    }
}