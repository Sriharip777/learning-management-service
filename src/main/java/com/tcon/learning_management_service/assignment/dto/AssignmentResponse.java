package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignmentResponse {

    private String id;

    private String title;

    private String description;

    private String teacherId;

    private String courseId;

    private LocalDate dueDate;

    private String status;

}