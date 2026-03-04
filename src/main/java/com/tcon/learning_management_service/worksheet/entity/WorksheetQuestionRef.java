package com.tcon.learning_management_service.worksheet.entity;

import lombok.Data;

@Data
public class WorksheetQuestionRef {

    private String questionMasterId;
    private String questionVersionId;

    private Integer orderIndex;
    private Integer marks;
}