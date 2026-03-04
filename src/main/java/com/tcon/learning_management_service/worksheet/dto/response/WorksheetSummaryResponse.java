package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Data;

@Data
public class WorksheetSummaryResponse {

    private String id;
    private String title;

    private String subjectId;
    private String gradeId;

    private String status;
}