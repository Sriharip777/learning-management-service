package com.tcon.learning_management_service.course.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "topics")
public class Topic {
    @Id
    private String id;
    private String subjectId;   // belongs to which subject
    private String name;        // "Linear Equations", "Quadratic Equations"
    private String description;
    private Boolean isActive;
}
