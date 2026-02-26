package com.tcon.learning_management_service.assignment.dto;

import com.tcon.learning_management_service.assignment.entity.AssignmentStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignmentResponse {

    private String id;

    private String title;

    private String description;

    private String teacherId;

    private LocalDate dueDate;

    private AssignmentStatus status;   // ✅ ENUM instead of String
}