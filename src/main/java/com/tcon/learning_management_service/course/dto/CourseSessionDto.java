package com.tcon.learning_management_service.course.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSessionDto {

    private String title;
    private String description;
    private List<String> topics;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant scheduledStartTime;
    private Integer durationMinutes;
}