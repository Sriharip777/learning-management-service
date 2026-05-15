package com.tcon.learning_management_service.availability.dto;

import lombok.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAvailabilityRequest {
    private Map<DayOfWeek, List<WeeklyTimeSlotDto>> weeklyAvailability;
    private Integer bufferTimeMinutes;
    private Integer maxSessionsPerDay;
    private Boolean oneOnOneEnabled;
    private Boolean groupEnabled;
    private WeeklyPatternDto weeklyPattern;
}