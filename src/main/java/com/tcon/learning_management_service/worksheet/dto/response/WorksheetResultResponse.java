package com.tcon.learning_management_service.worksheet.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorksheetResultResponse {

    // ✅ NEW FIELD ADDED
    private String worksheetId;

    // ✅ EXISTING (unchanged)
    private int totalQuestions;
    private int correctAnswers;
    private int wrongAnswers;
    private double scorePercentage;

    private List<QuestionResult> results;

    @Data
    @Builder
    public static class QuestionResult {

        private String questionId;
        private String question;

        private String correctAnswer;   // 🔥 fixed
        private String studentAnswer;   // 🔥 fixed

        private String reason;
        private boolean isCorrect;
    }
}