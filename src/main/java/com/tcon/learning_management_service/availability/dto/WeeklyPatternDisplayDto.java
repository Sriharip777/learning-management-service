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
    private List<Integer> days;
    private List<String> dayNames;

    private String timeStart;
    private String timeEnd;

    private String displayTimeStart;
    private String displayTimeEnd;

    private String timezoneAbbreviation;
    private String timezoneId;
    private String utcOffset;
}