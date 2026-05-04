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
    private String timezoneId;            // e.g. "Asia/Kolkata"
    private String displayName;           // e.g. "Asia/Kolkata"
    private String currentTime;           // e.g. "03:30 PM IST"
    private String currentOffset;         // e.g. "+05:30"
    private String timezoneAbbreviation;  // e.g. "IST"
}