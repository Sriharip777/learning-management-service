package com.tcon.learning_management_service.assignment.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "assignment_attempts")
public class AssignmentAttempt {

    @Id
    private String id;

    private String assignmentId;
    private String studentId;

    // Start date & time
    private LocalDateTime attendedAt;

    // Last activity time
    private LocalDateTime lastActiveAt;

    // End date & time (submission time)
    private LocalDateTime submittedAt;

    // Total time spent in seconds
    private Long timeSpentSeconds;

    // Late submission indicator
    private Boolean lateSubmission = false;

    // Total questions in assignment
    private Integer totalQuestions;

    // Questions attempted by student
    private Integer attemptedQuestions;

    // Questions left unanswered
    private Integer remainingQuestions;

    // Questions marked for review
    private Integer markedForReview;

    // Questions flagged by student
    private Integer flaggedQuestions;
}