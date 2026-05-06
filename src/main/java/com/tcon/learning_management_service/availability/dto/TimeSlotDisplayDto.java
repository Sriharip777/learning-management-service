package com.tcon.learning_management_service.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDisplayDto {
    private String startTime;
    private String endTime;
    private Boolean isAvailable;
    private SessionMode mode;

    private String displayStartTime;
    private String displayEndTime;
    private String timezoneAbbreviation;
    private String timezoneId;
    private String utcOffset;

    private String displayStartDateTime;
    private String displayEndDateTime;
}