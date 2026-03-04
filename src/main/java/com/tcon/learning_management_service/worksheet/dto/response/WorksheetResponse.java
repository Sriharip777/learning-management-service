package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Data;

@Data
public class WorksheetResponse {

    private String id;
    private String title;

    private Integer currentVersion;
    private String status;
}