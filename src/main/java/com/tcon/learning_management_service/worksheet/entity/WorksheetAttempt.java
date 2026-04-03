package com.tcon.learning_management_service.worksheet.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "worksheet_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorksheetAttempt {

    @Id
    private String attemptId;

    private String worksheetId;
    private String studentId;

    private Integer totalQuestions;
    private Integer correctAnswers;

    // ✅ KEEP THIS (better than Integer)
    private double score;

    private LocalDateTime submittedAt;

    // ✅ VERY IMPORTANT
    private Map<String, String> answers; // questionId -> selectedOption
}