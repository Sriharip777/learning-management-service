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
public class WeeklyPatternDto {
    private Boolean enabled;
    // ✅ CHANGED: multiple days instead of day1/day2
    private List<Integer> days;       // [1, 3] = Monday, Wednesday (0=Sun...6=Sat)
    private String timeStart;         // "HH:mm"
    private String timeEnd;           // "HH:mm"
}
