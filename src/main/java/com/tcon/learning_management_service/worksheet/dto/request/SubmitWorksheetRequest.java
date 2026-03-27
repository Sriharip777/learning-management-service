package com.tcon.learning_management_service.worksheet.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class SubmitWorksheetRequest {

    private String worksheetId;
    private String studentId;

    // questionId -> selected option (safe for shuffle)
    private Map<String, String> answers;
}