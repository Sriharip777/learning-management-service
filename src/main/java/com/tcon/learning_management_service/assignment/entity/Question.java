package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    private String questionText;

    private List<String> options;      // REQUIRED

    private String correctAnswer;      // Correct answer

    private String reason;             // Explanation for the correct answer

    private String teacherId;          // REQUIRED
}