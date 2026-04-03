package com.tcon.learning_management_service.worksheet.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WorksheetRejectedEvent {

    private String worksheetId;
    private String teacherId;
    private String comments;
}