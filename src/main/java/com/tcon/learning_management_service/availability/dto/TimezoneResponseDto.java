package com.tcon.learning_management_service.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimezoneResponseDto {
    private String stateName;       // "New York"
    private String stateCode;       // "NY"
    private String timezoneId;      // "America/New_York" ← use this for API calls
    private String timezoneLabel;   // "Eastern Time (ET)"
    private String utcOffset;       // "UTC-5/UTC-4"
    private String currentTime;     // "10:30 AM EST" ← current local time in this zone
}

