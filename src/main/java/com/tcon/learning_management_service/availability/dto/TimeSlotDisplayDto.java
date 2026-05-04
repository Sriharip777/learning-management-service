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

    private String startTime;             // stored time
    private String endTime;               // stored time
    private Boolean isAvailable;
    private SessionMode mode;

    private String displayStartTime;      // viewer timezone
    private String displayEndTime;        // viewer timezone
    private String timezoneAbbreviation;  // e.g. IST / EDT
    private String timezoneId;            // e.g. Asia/Kolkata
    private String utcOffset;             // e.g. +05:30
}