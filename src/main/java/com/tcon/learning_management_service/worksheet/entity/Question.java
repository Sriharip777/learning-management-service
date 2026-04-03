package com.tcon.learning_management_service.worksheet.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {

    @Id
    private String id;

    private String questionMasterId;
    private String questionVersionId;

    private String questionText;

    private List<String> options;

    private Integer correctAnswerIndex;

    private String reason;

    // 🔥 ADDED (from colleague)
    private String worksheetId;
}