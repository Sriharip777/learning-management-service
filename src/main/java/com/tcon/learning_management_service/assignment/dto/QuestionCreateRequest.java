package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;
import java.util.List;   // ✅ ADD THIS IMPORT

@Data
public class QuestionCreateRequest {

    private String questionText;

    private List<String> options;

    private String correctAnswer;

    private String teacherId;
}