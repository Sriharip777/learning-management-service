package com.tcon.learning_management_service.worksheet.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionsFlaggedEvent {

    private String worksheetId;

    private String flaggedBy;

    private String reason;

    private LocalDateTime flaggedAt;
}

