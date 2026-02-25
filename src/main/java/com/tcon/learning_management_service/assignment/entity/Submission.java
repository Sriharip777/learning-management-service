package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "submissions")
public class Submission {

    @Id
    private String id;

    private String assignmentId;

    private String studentId;

    private List<Answer> answers;

    private int score;

    private int totalMarks;

    private String status; // SUBMITTED, EVALUATED
}