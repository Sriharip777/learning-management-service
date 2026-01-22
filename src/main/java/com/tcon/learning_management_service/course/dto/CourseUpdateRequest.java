package com.tcon.learning_management_service.course.dto;

import com.tcon.learning_management_service.course.entity.CourseSchedule;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
public class CourseUpdateRequest {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @Size(min = 20, max = 5000, message = "Description must be between 20 and 5000 characters")
    private String description;

    private CourseStatus status;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private BigDecimal pricePerSession;

    @Min(value = 1, message = "Minimum students must be at least 1")
    private Integer minStudents;

    @Min(value = 1, message = "Maximum students must be at least 1")
    @Max(value = 100, message = "Maximum students cannot exceed 100")
    private Integer maxStudents;

    private CourseSchedule schedule;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<String> prerequisites;
    private List<String> learningOutcomes;
    private String thumbnailUrl;
    private List<String> materialUrls;
    private Boolean isDemoAvailable;
    private Integer demoSessionDuration;
}
