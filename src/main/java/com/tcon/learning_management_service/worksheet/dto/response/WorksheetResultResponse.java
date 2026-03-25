package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorksheetResultResponse {

    private int totalQuestions;
    private int correctAnswers;
    private int score;

    private List<QuestionResult> results;

    @Data
    @Builder
    public static class QuestionResult {
        private Long questionId;
        private String question;
        private int correctAnswer;
        private int studentAnswer;
        private String reason;
        private boolean isCorrect;
    }
}