package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AssignmentCreateRequest {

    private String title;

    private String description;

    private String teacherId;

    private List<String> questionIds;

    private LocalDateTime dueDate;
}