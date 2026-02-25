package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.SubmissionRequest;
import com.tcon.learning_management_service.assignment.entity.Answer;
import com.tcon.learning_management_service.assignment.entity.Submission;
import com.tcon.learning_management_service.assignment.event.AssignmentEventPublisher;
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import com.tcon.learning_management_service.assignment.repository.SubmissionRepository;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final EvaluationService evaluationService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentEventPublisher eventPublisher;


    /**
     * Student submits or resubmits assignment
     */
    public Submission submitAssignment(SubmissionRequest request)
    {

        // Validate assignment exists
        assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: "
                                        + request.getAssignmentId()));

        Optional<Submission> existingSubmission =
                submissionRepository
                        .findByAssignmentIdAndStudentId(
                                request.getAssignmentId(),
                                request.getStudentId());

        Submission submission =
                existingSubmission.orElse(new Submission());


        submission.setAssignmentId(request.getAssignmentId());
        submission.setStudentId(request.getStudentId());


        // Evaluate answers
        List<Answer> answers =
                evaluationService.evaluate(request);

        submission.setAnswers(answers);


        int score = answers.stream()
                .mapToInt(Answer::getMarks)
                .sum();

        submission.setScore(score);


        int totalMarks =
                evaluationService.getTotalMarks(
                        request.getAssignmentId());

        submission.setTotalMarks(totalMarks);


        submission.setStatus("EVALUATED");


        Submission savedSubmission =
                submissionRepository.save(submission);


        // Kafka Events
        eventPublisher.publishAssignmentSubmitted(savedSubmission);
        eventPublisher.publishAssignmentEvaluated(savedSubmission);


        return savedSubmission;
    }



    /**
     * Student view own submission
     */
    public Submission getSubmission(
            String assignmentId,
            String studentId)
    {

        return submissionRepository
                .findByAssignmentIdAndStudentId(
                        assignmentId,
                        studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Submission not found"));
    }



    /**
     * Teacher view results
     */
    public List<Submission> getResults(
            String assignmentId)
    {

        assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: "
                                        + assignmentId));

        return submissionRepository
                .findByAssignmentId(assignmentId);
    }

}