package com.tcon.learning_management_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "courses")
public class Course {

    @Id
    private String id;

    @Indexed
    private String teacherId;

    private String title;
    private String description;
    private CourseCategory category;
    private CourseStatus status;

    private Integer durationMinutes;
    private Double price;
    private String currency;

    private Boolean isRecurring;
    private String schedule; // e.g., "Mon,Wed,Fri 10:00 AM"

    private List<String> tags;
    private String level; // BEGINNER, INTERMEDIATE, ADVANCED

    private Integer maxStudents;
    private Integer enrolledStudents;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
