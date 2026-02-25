package com.tcon.learning_management_service.assignment.controller;

import com.tcon.learning_management_service.assignment.dto.SubmissionRequest;
import com.tcon.learning_management_service.assignment.entity.Submission;
import com.tcon.learning_management_service.assignment.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;


    // Student submit assignment
    @PostMapping
    public Submission submitAssignment(
            @RequestBody SubmissionRequest request)
    {
        return submissionService
                .submitAssignment(request);
    }


    // Student view own submission
    @GetMapping("/{assignmentId}/{studentId}")
    public Submission getSubmission(
            @PathVariable String assignmentId,
            @PathVariable String studentId)
    {
        return submissionService
                .getSubmission(
                        assignmentId,
                        studentId);
    }

}