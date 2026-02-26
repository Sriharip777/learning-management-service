package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.dto.AssignmentCreateRequest;
import com.tcon.learning_management_service.assignment.dto.AssignStudentsRequest;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.entity.Submission;
import com.tcon.learning_management_service.assignment.service.AssignmentService;
import com.tcon.learning_management_service.assignment.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;


    /**
     * Teacher creates assignment
     * Assignment must contain questionIds (no empty assignment allowed)
     */
    @PostMapping
    public Assignment createAssignment(
            @RequestBody AssignmentCreateRequest request) {
        return assignmentService.createAssignment(request);
    }


    /**
     * Teacher assigns students to assignment
     */
    @PostMapping("/{assignmentId}/assign")
    public Assignment assignStudents(
            @PathVariable String assignmentId,
            @RequestBody AssignStudentsRequest request) {
        return assignmentService.assignStudents(assignmentId, request);
    }


    /**
     * Student views assigned assignments
     */
    @GetMapping("/student/{studentId}")
    public List<Assignment> getStudentAssignments(
            @PathVariable String studentId) {
        return assignmentService.getAssignmentsForStudent(studentId);
    }


    /**
     * Teacher views created assignments
     */
    @GetMapping("/teacher/{teacherId}")
    public List<Assignment> getTeacherAssignments(
            @PathVariable String teacherId) {
        return assignmentService.getAssignmentsForTeacher(teacherId);
    }


    /**
     * Teacher views results of assignment
     */
    @GetMapping("/{assignmentId}/results")
    public List<Submission> getResults(
            @PathVariable String assignmentId) {
        return submissionService.getResults(assignmentId);
    }
}