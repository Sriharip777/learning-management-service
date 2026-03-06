package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "submissions")
public class Submission {

    @Id
    private String id;

    private String assignmentId;

    private String studentId;

    private List<Answer> answers;

    private int score;

    private int totalMarks;

    private String status; // SUBMITTED, EVALUATED

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    // Time breakdown
    private long timeTakenDays;

    private long timeTakenHours;

    private long timeTakenMinutes;

    private long timeTakenSeconds;

    // Question tracking
    private List<QuestionAttempt> questionAttempts;

    private int attemptedQuestions;

    private int remainingQuestions;

    private int markedForReview;

    private int flaggedQuestions;
}