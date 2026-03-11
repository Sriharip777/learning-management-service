package com.tcon.learning_management_service.course.dto;

import com.tcon.learning_management_service.course.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSearchDto {
    private String keyword;

    // NEW: replaces category/tags
    private String gradeId;
    private String subjectId;
    private List<String> topicIds;

    private String teacherId;
    private List<CourseStatus> statuses;
    private String gradeLevel;
    private String difficulty;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double minRating;
    private Boolean isDemoAvailable;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 20;

    @Builder.Default
    private String sortBy = "createdAt";

    @Builder.Default
    private String sortDirection = "DESC";
}
