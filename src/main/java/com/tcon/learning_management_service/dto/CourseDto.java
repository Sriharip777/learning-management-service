package com.tcon.learning_management_service.dto;

import com.tcon.learning_management_service.entity.CourseCategory;
import com.tcon.learning_management_service.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDto {
    private String id;
    private String teacherId;
    private String teacherName;
    private String title;
    private String description;
    private CourseCategory category;
    private CourseStatus status;
    private Integer durationMinutes;
    private Double price;
    private String currency;
    private Boolean isRecurring;
    private String schedule;
    private List<String> tags;
    private String level;
    private Integer maxStudents;
    private Integer enrolledStudents;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
