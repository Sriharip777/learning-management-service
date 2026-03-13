package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
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

    // Curriculum hierarchy
    private String gradeId;

    private String subjectId;

    private String topicId;

    // Students assigned
    private List<String> studentIds = new ArrayList<>();

    // Questions inside assignment
    private List<String> questionIds = new ArrayList<>();

    // Deadline
    private LocalDateTime dueDate;

    private AssignmentStatus status;
}