package com.tcon.learning_management_service.availability.dto;

import com.tcon.learning_management_service.availability.entity.WeeklyTimeSlot;
import lombok.*;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAvailabilityDto {
    private String id;
    private String teacherId;
    private Map<DayOfWeek, List<WeeklyTimeSlot>> weeklyAvailability;
    private Integer bufferTimeMinutes;
    private Integer maxSessionsPerDay;
    private Boolean oneOnOneEnabled;
    private Boolean groupEnabled;
    private WeeklyPatternDto weeklyPattern;
}