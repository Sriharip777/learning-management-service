package com.tcon.learning_management_service.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Weekly pattern config: same two days & same time every week
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPatternDto {
    private Boolean enabled;      // true/false
    private Integer day1;         // 0-6 (JS getDay): 0=Sunday, 1=Monday...
    private Integer day2;         // 0-6
    private String timeStart;     // "HH:mm"
    private String timeEnd;       // "HH:mm"
}