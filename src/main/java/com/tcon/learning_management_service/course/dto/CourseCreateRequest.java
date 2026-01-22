package com.tcon.learning_management_service.course.dto;

import com.tcon.learning_management_service.course.entity.CourseCategory;
import com.tcon.learning_management_service.course.entity.CourseSchedule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private CourseCategory category;

    private List<String> tags;

    @NotNull(message = "Price per session is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePerSession;

    @Builder.Default
    private String currency = "USD";

    @Min(value = 1, message = "Minimum students must be at least 1")
    private Integer minStudents;

    @Min(value = 1, message = "Maximum students must be at least 1")
    @Max(value = 100, message = "Maximum students cannot exceed 100")
    private Integer maxStudents;

    @NotBlank(message = "Grade level is required")
    private String gradeLevel;

    @NotBlank(message = "Difficulty is required")
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED", message = "Difficulty must be BEGINNER, INTERMEDIATE, or ADVANCED")
    private String difficulty;

    @Valid
    private CourseSchedule schedule;

    @NotNull(message = "Start date is required")
    @Future(message = "Start date must be in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Total sessions is required")
    @Min(value = 1, message = "Total sessions must be at least 1")
    private Integer totalSessions;

    private List<String> prerequisites;
    private List<String> learningOutcomes;
    private String thumbnailUrl;
    private List<String> materialUrls;

    @Builder.Default
    private Boolean isDemoAvailable = false;

    private Integer demoSessionDuration;
}
