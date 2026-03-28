package com.tcon.learning_management_service.worksheet.dto.request;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignWorksheetRequest {

    private String worksheetId;
    private String teacherId;
    private List<String> studentIds;
    private LocalDateTime dueDate;
}