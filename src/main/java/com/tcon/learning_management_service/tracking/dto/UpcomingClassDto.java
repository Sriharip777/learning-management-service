package com.tcon.learning_management_service.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpcomingClassDto {
    private String id;
    private String studentId;
    private String student;
    private String subject;
    private Instant scheduledStartTime;
}