package com.tcon.learning_management_service.worksheet.dto.request;

import lombok.Data;

@Data
public class UpdateWorksheetRequest {

    private String worksheetId;

    private String title;

    private String instructions;
    private String difficultyLevel;
    private Integer estimatedDuration;
}