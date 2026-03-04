package com.tcon.learning_management_service.worksheet.entity;

import lombok.Data;

@Data
public class WorksheetMetadata {

    private String instructions;
    private String difficultyLevel;
    private Integer estimatedDuration;
}