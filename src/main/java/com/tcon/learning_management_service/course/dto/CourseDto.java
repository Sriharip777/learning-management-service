package com.tcon.learning_management_service.course.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcon.learning_management_service.course.entity.CourseCategory;
import com.tcon.learning_management_service.course.entity.CourseSchedule;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private List<String> tags;
    private CourseStatus status;
    private BigDecimal pricePerSession;
    private String currency;
    private Integer minStudents;
    private Integer maxStudents;
    private Integer currentEnrollments;
    private String gradeLevel;
    private String difficulty;
    private CourseSchedule schedule;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private Integer totalSessions;
    private Integer completedSessions;
    private List<String> prerequisites;
    private List<String> learningOutcomes;
    private String thumbnailUrl;
    private List<String> materialUrls;
    private Boolean isDemoAvailable;
    private Integer demoSessionDuration;
    private Double rating;
    private Integer totalReviews;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
