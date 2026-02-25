package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignmentCreateRequest {

    private String title;

    private String description;

    private String teacherId;

    private LocalDate dueDate;
}