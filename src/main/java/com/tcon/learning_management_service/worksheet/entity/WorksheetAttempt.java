package com.tcon.learning_management_service.worksheet.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import com.tcon.learning_management_service.worksheet.entity.AttemptType;
import com.tcon.learning_management_service.worksheet.entity.AttemptStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "worksheet_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorksheetAttempt {

    @Id
    private String attemptId;

    private String worksheetId;
    private String studentId;

    // 🔥 NEW: attempt type
    private AttemptType attemptType; // ASSIGNED / SELF_PRACTICE

    // 🔥 NEW: assignment reference (optional)
    private String assignedByTeacherId;

    // 🔥 NEW: attempt lifecycle
    private AttemptStatus status; // IN_PROGRESS / SUBMITTED

    // 🔥 NEW: tracking
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;

    private Integer totalQuestions;
    private Integer correctAnswers;

    private double score;

    // questionId -> selectedOption
    private Map<String, String> answers;
}