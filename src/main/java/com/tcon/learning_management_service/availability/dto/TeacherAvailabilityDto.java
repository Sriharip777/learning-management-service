package com.tcon.learning_management_service.availability.dto;

import com.tcon.learning_management_service.availability.entity.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String timezone;
    private Map<DayOfWeek, List<TimeSlot>> weeklyAvailability;
    private Integer bufferTimeMinutes;
    private Integer maxSessionsPerDay;
}
