package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.QuestionAttemptRequest;
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

    /**
     * Student starts assignment
     */
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

    /**
     * Track student activity (heartbeat)
     */
    public void trackActivity(String assignmentId, String studentId) {

        Submission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Submission not found"));

        submission.setEndTime(LocalDateTime.now());

        submissionRepository.save(submission);
    }

    /**
     * Student submits assignment
     */
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

        int score = answers.stream()
                .mapToInt(Answer::getMarks)
                .sum();

        submission.setScore(score);

        int totalMarks =
                evaluationService.getTotalMarks(request.getAssignmentId());

        submission.setTotalMarks(totalMarks);

        LocalDateTime startTime = submission.getStartTime();
        LocalDateTime endTime = LocalDateTime.now();

        submission.setEndTime(endTime);

        Duration duration = Duration.between(startTime, endTime);

        long totalSeconds = Math.max(duration.getSeconds(), 0);

        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        submission.setTimeTakenDays(days);
        submission.setTimeTakenHours(hours);
        submission.setTimeTakenMinutes(minutes);
        submission.setTimeTakenSeconds(seconds);

        if (request.getQuestionAttempts() != null) {

            List<QuestionAttemptRequest> attempts = request.getQuestionAttempts();

            List<QuestionAttempt> questionAttempts = attempts.stream()
                    .map(a -> {
                        QuestionAttempt qa = new QuestionAttempt();
                        qa.setQuestionId(a.getQuestionId());
                        qa.setSelectedAnswer(a.getSelectedAnswer());
                        qa.setAttempted(a.isAttempted());
                        qa.setMarkedForReview(a.isMarkedForReview());
                        qa.setFlagged(a.isFlagged());
                        qa.setCorrect(a.isCorrect());
                        return qa;
                    })
                    .collect(Collectors.toList());

            submission.setQuestionAttempts(questionAttempts);

            int attempted = (int) attempts.stream()
                    .filter(QuestionAttemptRequest::isAttempted)
                    .count();

            int review = (int) attempts.stream()
                    .filter(QuestionAttemptRequest::isMarkedForReview)
                    .count();

            int flagged = (int) attempts.stream()
                    .filter(QuestionAttemptRequest::isFlagged)
                    .count();

            int totalQuestions = attempts.size();
            int remaining = totalQuestions - attempted;

            submission.setAttemptedQuestions(attempted);
            submission.setRemainingQuestions(remaining);
            submission.setMarkedForReview(review);
            submission.setFlaggedQuestions(flagged);
        }

        submission.setStatus("EVALUATED");

        Submission savedSubmission = submissionRepository.save(submission);

        eventPublisher.publishAssignmentSubmitted(savedSubmission);
        eventPublisher.publishAssignmentEvaluated(savedSubmission);

        return savedSubmission;
    }

    /**
     * Student view own submission
     */
    public Submission getSubmission(String assignmentId, String studentId) {

        return submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Submission not found"));
    }

    /**
     * Teacher view results
     */
    public List<Submission> getResults(String assignmentId) {

        assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + assignmentId));

        return submissionRepository.findByAssignmentId(assignmentId);
    }

    /**
     * Parent view results for a student
     */
    public List<Submission> getResultsByStudent(String studentId) {

        return submissionRepository.findByStudentId(studentId);
    }
}