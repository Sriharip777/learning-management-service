package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.SubmissionRequest;
import com.tcon.learning_management_service.assignment.entity.Answer;
import com.tcon.learning_management_service.assignment.entity.QuestionAttempt;
import com.tcon.learning_management_service.assignment.entity.Submission;
import com.tcon.learning_management_service.assignment.event.AssignmentEventPublisher;
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import com.tcon.learning_management_service.assignment.repository.SubmissionRepository;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final EvaluationService evaluationService;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentEventPublisher eventPublisher;

    public Submission startAssignment(String assignmentId, String studentId) {

        assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Assignment not found: " + assignmentId));

        Optional<Submission> existingSubmission =
                submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId);

        if (existingSubmission.isPresent()) {
            return existingSubmission.get();
        }

        Submission submission = new Submission();
        submission.setAssignmentId(assignmentId);
        submission.setStudentId(studentId);
        submission.setStartTime(LocalDateTime.now());
        submission.setStatus("STARTED");

        return submissionRepository.save(submission);
    }

    public void trackActivity(String assignmentId, String studentId) {

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Submission not found"));

        submission.setEndTime(LocalDateTime.now());
        submissionRepository.save(submission);
    }

    public Submission submitAssignment(SubmissionRequest request) {

        assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + request.getAssignmentId()));

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(
                        request.getAssignmentId(),
                        request.getStudentId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Submission not started"));

        List<Answer> answers = evaluationService.evaluate(request);
        submission.setAnswers(answers);

        int score = answers.stream().mapToInt(Answer::getMarks).sum();
        submission.setScore(score);

        int totalMarks = evaluationService.getTotalMarks(request.getAssignmentId());
        submission.setTotalMarks(totalMarks);

        LocalDateTime startTime = submission.getStartTime();
        LocalDateTime endTime = LocalDateTime.now();

        submission.setEndTime(endTime);

        Duration duration = Duration.between(startTime, endTime);
        long totalSeconds = Math.max(duration.getSeconds(), 0);

        submission.setTimeTakenDays(totalSeconds / (24 * 3600));
        submission.setTimeTakenHours((totalSeconds % (24 * 3600)) / 3600);
        submission.setTimeTakenMinutes((totalSeconds % 3600) / 60);
        submission.setTimeTakenSeconds(totalSeconds % 60);

        submission.setStatus("EVALUATED");

        Submission savedSubmission = submissionRepository.save(submission);

        eventPublisher.publishAssignmentSubmitted(savedSubmission);
        eventPublisher.publishAssignmentEvaluated(savedSubmission);

        return savedSubmission;
    }

    public Submission getSubmission(String assignmentId, String studentId) {

        return submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Submission not found"));
    }

    public List<Submission> getResults(String assignmentId) {

        assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + assignmentId));

        return submissionRepository.findByAssignmentId(assignmentId);
    }

    public List<Submission> getResultsForParent(String parentId, String studentId, String role) {

        if (!"PARENT".equals(role)) {
            throw new RuntimeException("Access denied");
        }

        List<String> childrenIds = getChildrenIds(parentId);

        if (!childrenIds.contains(studentId)) {
            throw new RuntimeException("Not allowed");
        }

        return submissionRepository.findByStudentId(studentId);
    }

    private List<String> getChildrenIds(String parentId) {

        return submissionRepository.findAll()
                .stream()
                .map(Submission::getStudentId)
                .distinct()
                .toList();
    }

    public List<Submission> getResultsByStudent(String studentId) {
        return submissionRepository.findByStudentId(studentId);
    }
}