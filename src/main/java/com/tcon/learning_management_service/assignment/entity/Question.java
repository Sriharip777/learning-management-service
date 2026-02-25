package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "questions")
public class Question {

    @Id
    private String id;

    private String assignmentId;

    private String questionText;

    private String correctAnswer;

    private int marks;
}