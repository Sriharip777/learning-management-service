package com.tcon.learning_management_service.availability.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimeSlotDto {
    private String startTime;   // "HH:mm[:ss]" local
    private String endTime;     // "HH:mm[:ss]" local
    private Boolean isAvailable;
    private SessionMode mode;
}