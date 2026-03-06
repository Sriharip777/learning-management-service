package com.tcon.learning_management_service.assignment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class SubmissionRequest {

    private String assignmentId;

    private String studentId;

    private List<AnswerRequest> answers;

    private LocalDateTime startTime;

    private List<QuestionAttemptRequest> questionAttempts;
}