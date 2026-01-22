package com.tcon.learning_management_service.course.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private List<String> tags;

    @Builder.Default
    private CourseStatus status = CourseStatus.DRAFT;

    private BigDecimal pricePerSession;
    private String currency;

    private Integer minStudents;
    private Integer maxStudents;

    @Builder.Default
    private Integer currentEnrollments = 0;

    private String gradeLevel;
    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED

    private CourseSchedule schedule;

    private LocalDate startDate;
    private LocalDate endDate;

    private Integer totalSessions;
    private Integer completedSessions;

    @Builder.Default
    private List<String> prerequisites = new ArrayList<>();

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();

    private String thumbnailUrl;

    @Builder.Default
    private List<String> materialUrls = new ArrayList<>();

    private Boolean isDemoAvailable;
    private Integer demoSessionDuration;

    @Builder.Default
    private Double rating = 0.0;

    @Builder.Default
    private Integer totalReviews = 0;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;
    private String updatedBy;
}
