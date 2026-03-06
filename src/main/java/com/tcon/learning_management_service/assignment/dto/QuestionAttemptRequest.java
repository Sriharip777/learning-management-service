package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

@Data
public class QuestionAttemptRequest {

    private String questionId;

    private String selectedAnswer;

    private boolean attempted;

    private boolean markedForReview;

    private boolean flagged;

    private boolean correct;
}