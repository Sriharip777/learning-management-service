package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

@Data
public class ResultResponse {

    private String studentId;

    private int score;

    private int totalMarks;

}