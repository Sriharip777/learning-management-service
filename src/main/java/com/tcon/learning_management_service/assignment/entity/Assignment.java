package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "assignments")
public class Assignment {

    @Id
    private String id;

    private String title;

    private String description;

    private String teacherId;

    private List<String> studentIds = new ArrayList<>();

    private List<String> questionIds;

    private LocalDate dueDate;

    private AssignmentStatus status;   // ✅ ENUM instead of String
}