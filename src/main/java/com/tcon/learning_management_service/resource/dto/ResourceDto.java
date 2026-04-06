package com.tcon.learning_management_service.resource.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDto {
    private String id;
    private String title;
    private String description;

    private String gradeId;
    private String gradeName;

    private String subjectId;
    private String subjectName;

    private String topicId;
    private String topicName;

    private List<ResourceFileDto> files;

    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}