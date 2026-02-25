package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

@Data
public class QuestionCreateRequest {

    private String assignmentId;

    private String questionText;

    private String correctAnswer;

    private int marks;
}