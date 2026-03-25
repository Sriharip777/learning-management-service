package com.tcon.learning_management_service.worksheet.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class SubmitWorksheetRequest {

    private String worksheetId;
    private String studentId;

    // questionId (String) -> selected option index (0-3)
    private Map<String, Integer> answers;
}