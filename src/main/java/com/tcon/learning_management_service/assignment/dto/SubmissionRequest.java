package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;
import java.util.List;

@Data
public class SubmissionRequest {

    private String assignmentId;

    private String studentId;

    private List<AnswerRequest> answers;

}