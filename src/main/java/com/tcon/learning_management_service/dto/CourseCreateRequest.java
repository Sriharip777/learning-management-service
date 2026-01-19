package com.tcon.learning_management_service.dto;
import com.tcon.learning_management_service.entity.CourseCategory;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "Teacher ID is required")
    private String teacherId;

    @NotBlank(message = "Title is required")
    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @NotBlank(message = "Description is required")
    @Size(min = 20, max = 2000, message = "Description must be between 20 and 2000 characters")
    private String description;

    @NotNull(message = "Category is required")
    private CourseCategory category;

    @NotNull(message = "Duration is required")
    @Min(value = 30, message = "Minimum duration is 30 minutes")
    @Max(value = 120, message = "Maximum duration is 120 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Recurring flag is required")
    private Boolean isRecurring;

    private String schedule;

    private List<String> tags;

    @NotBlank(message = "Level is required")
    private String level;

    @Min(value = 1, message = "At least 1 student required")
    private Integer maxStudents;
}
