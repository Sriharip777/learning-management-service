package com.tcon.learning_management_service.assignment.entity;

import lombok.Data;

@Data
public class Answer {

    private String questionId;

    private String answer;

    private boolean correct;

    private int marks;
}