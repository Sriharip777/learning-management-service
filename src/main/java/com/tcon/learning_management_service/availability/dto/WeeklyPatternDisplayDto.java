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
public class WeeklyPatternDisplayDto {

    private Boolean enabled;

    // Selected days as numbers and names
    private List<Integer> days;              // [1, 3]
    private List<String> dayNames;           // ["MONDAY", "WEDNESDAY"]

    // Original stored times (UTC / teacher's timezone)
    private String timeStart;               // "14:00"
    private String timeEnd;                 // "15:00"

    // Converted display times in viewer's selected timezone
    private String displayTimeStart;        // "09:30 AM"
    private String displayTimeEnd;          // "10:30 AM"
    private String timezoneAbbreviation;    // "EST"
    private String timezoneId;              // "America/New_York"
    private String stateName;              // "New York"
    private String stateCode;             // "NY"
    private String utcOffset;             // "UTC-5/UTC-4"
}

