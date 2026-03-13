package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentCreateRequest {

    private String title;

    private String description;

    private String teacherId;

    // Curriculum hierarchy
    private String gradeId;

    private String subjectId;

    private String topicId;

    // Questions included in the assignment
    private List<String> questionIds;

    // Assignment deadline
    private LocalDateTime dueDate;
}