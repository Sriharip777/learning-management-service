package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;
import java.util.List;

@Data
public class QuestionCreateRequest {

    private String questionText;

    private List<String> options;

    private String correctAnswer;

    private String reason;      // Explanation for the correct answer

    private String teacherId;
}