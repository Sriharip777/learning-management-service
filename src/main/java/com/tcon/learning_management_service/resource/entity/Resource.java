package com.tcon.learning_management_service.resource.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resources")
public class Resource {
    @Id
    private String id;

    private String title;
    private String description;

    private String gradeId;
    private String gradeName;

    private String subjectId;
    private String subjectName;

    private String topicId;
    private String topicName;

    private List<ResourceFile> files;

    private String uploadedBy;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;
    private Boolean isActive;
}