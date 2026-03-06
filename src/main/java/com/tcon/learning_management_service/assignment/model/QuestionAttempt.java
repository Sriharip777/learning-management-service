package com.tcon.learning_management_service.assignment.model;

import lombok.Data;

@Data
public class QuestionAttempt {

    private String questionId;

    // whether student attempted the question
    private boolean attempted;

    // student marked for review
    private boolean markedForReview;

    // student flagged the question
    private boolean flagged;
}