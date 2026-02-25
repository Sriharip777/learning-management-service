package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

@Data
public class AnswerRequest {

    private String questionId;

    private String answer;

}