package com.tcon.learning_management_service.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchDateAvailabilityRequest {

    private String teacherId;
    private String timezone;
    private Integer bufferTimeMinutes;
    private Boolean oneOnOneEnabled;
    private Boolean groupEnabled;
    private WeeklyPatternDto weeklyPattern;
    private List<DateSlotRequest> dateSlots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateSlotRequest {
        private String date; // yyyy-MM-dd in teacher local date
        private List<TimeSlotRequest> timeSlots;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlotRequest {
        private String startTime; // HH:mm:ss or HH:mm
        private String endTime;   // HH:mm:ss or HH:mm
        private Boolean isAvailable;
        private SessionMode mode;
    }
}