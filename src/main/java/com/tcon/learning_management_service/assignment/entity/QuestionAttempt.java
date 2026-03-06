package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;

@Data
public class QuestionAttempt {

    private String questionId;

    private String selectedAnswer;

    private boolean correct;

    private boolean attempted;

    private boolean markedForReview;

    private boolean flagged;
}