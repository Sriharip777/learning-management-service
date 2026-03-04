package com.tcon.learning_management_service.worksheet.dto.request;

import lombok.Data;

@Data
public class CreateWorksheetRequest {

    private String title;

    private String subjectId;
    private String gradeId;
    private String topicId;

    private String instructions;
    private String difficultyLevel;
    private Integer estimatedDuration;
}