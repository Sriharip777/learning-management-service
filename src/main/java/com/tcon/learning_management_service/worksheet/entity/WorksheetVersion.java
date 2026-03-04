package com.tcon.learning_management_service.worksheet.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document(collection = "worksheet_versions")
public class WorksheetVersion {

    @Id
    private String id;

    private String worksheetId;

    private Integer versionNumber;

    private WorksheetStatus status;

    private Integer totalMarks;
    private Integer questionCount;

    private List<WorksheetQuestionRef> questions;

    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}