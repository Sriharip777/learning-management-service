package com.tcon.learning_management_service.assignment.dto;

import com.tcon.learning_management_service.assignment.entity.AssignmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssignmentResponse {

    private String id;

    private String title;

    private String description;

    private String teacherId;

    private LocalDateTime dueDate;

    private AssignmentStatus status;
}