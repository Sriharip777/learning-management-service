package com.tcon.learning_management_service.worksheet.integration.dto;

import lombok.Data;

@Data
public class TopicDto {
    private String id;
    private String name;
    private int duration;
    private String subjectId;
}