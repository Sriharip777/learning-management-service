package com.tcon.learning_management_service.worksheet.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "worksheet_assignments")
public class WorksheetAssignment {

    @Id
    private String id;

    private String worksheetId;
    private String teacherId;
    private String studentId;

    private LocalDateTime assignedAt;
    private LocalDateTime dueDate;

    private boolean completed;
}