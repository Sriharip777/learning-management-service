package com.tcon.learning_management_service.course.entity;

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
public class CourseSession {

    private String title;
    private String description;

    private List<String> topics;

    private LocalDateTime scheduledStartTime;
    private Integer durationMinutes;
}
