package com.tcon.learning_management_service.worksheet.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "worksheets")
public class Worksheet {

    @Id
    private String id;

    private String title;

    private String subjectId;
    private String gradeId;
    private String topicId;

    private String createdBy;

    private Integer currentVersion;

    private WorksheetStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}