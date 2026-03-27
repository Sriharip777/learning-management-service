package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Data;

import java.util.List;

@Data
public class StudentQuestionResponse {

    private String questionId;
    private String questionText;
    private List<String> options;
}