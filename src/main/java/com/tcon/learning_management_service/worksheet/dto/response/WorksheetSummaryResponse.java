package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorksheetSummaryResponse {

    private String id;
    private String title;

    private String subjectId;
    private String gradeId;
    private String topicId;

    private String SubjectName;
    private String GradeName;
    private String TopicName;

    private String status;

    // ✅ ADDED (for teacher assignment history)
    private Integer assignedCount;
    private Integer completedCount;
    private Integer pendingCount;
    private LocalDateTime lastAssignedAt;
}