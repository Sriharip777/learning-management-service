package com.tcon.learning_management_service.course.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "subjects")
public class Subject {
    @Id
    private String id;
    private String gradeId;     // belongs to which grade
    private String name;        // "Mathematics", "Science"
    private String description;
    private Boolean isActive;
}
