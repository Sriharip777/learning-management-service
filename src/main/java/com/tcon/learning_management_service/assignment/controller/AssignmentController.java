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


    // Teacher creates assignment
    @PostMapping
    public Assignment createAssignment(
            @RequestBody AssignmentCreateRequest request)
    {
        return assignmentService.createAssignment(request);
    }


    // Teacher assigns students
    @PostMapping("/{assignmentId}/assign")
    public Assignment assignStudents(
            @PathVariable String assignmentId,
            @RequestBody AssignStudentsRequest request)
    {
        return assignmentService.assignStudents(
                assignmentId,
                request);
    }


    // Student view assignments
    @GetMapping("/student/{studentId}")
    public List<Assignment> getStudentAssignments(
            @PathVariable String studentId)
    {
        return assignmentService
                .getAssignmentsForStudent(studentId);
    }


    // Teacher view assignments
    @GetMapping("/teacher/{teacherId}")
    public List<Assignment> getTeacherAssignments(
            @PathVariable String teacherId)
    {
        return assignmentService
                .getAssignmentsForTeacher(teacherId);
    }


    // Teacher view results
    @GetMapping("/{assignmentId}/results")
    public List<Submission> getResults(
            @PathVariable String assignmentId)
    {
        return submissionService
                .getResults(assignmentId);
    }

}