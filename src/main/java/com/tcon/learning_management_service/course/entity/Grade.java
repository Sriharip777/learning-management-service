package com.tcon.learning_management_service.course.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "grades")
public class Grade {
    @Id
    private String id;
    private String name;        // "Grade 8", "Grade 9"
    private Integer order;      // for sorting in frontend
    private Boolean isActive;
}
