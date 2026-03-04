package com.tcon.learning_management_service.worksheet.event.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorksheetPublishedEvent {

    private String worksheetId;

    private Integer version;

    private String publishedBy;

    private LocalDateTime publishedAt;
}