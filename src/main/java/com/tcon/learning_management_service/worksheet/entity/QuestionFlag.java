package com.tcon.learning_management_service.worksheet.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Document(collection = "question_flags")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionFlag {

    @Id
    private String id;

    @NotBlank(message = "Question Master ID cannot be blank")
    private String questionMasterId;

    @NotBlank(message = "Question Version ID cannot be blank")
    private String questionVersionId;

    @NotBlank(message = "Flagged By (Teacher ID) cannot be blank")
    private String flaggedBy; // teacherId

    @NotBlank(message = "Comment cannot be blank")
    private String comment;

    @NotNull(message = "Flagged At timestamp cannot be null")
    private LocalDateTime flaggedAt;
}