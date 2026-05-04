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
    private String timezoneId;
    private String displayName;
    private String currentTime;
    private String currentOffset;
    private String timezoneAbbreviation;
}