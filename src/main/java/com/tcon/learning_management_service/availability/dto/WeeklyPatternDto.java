package com.tcon.learning_management_service.availability.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyPatternDto {
    private Boolean enabled;
    private List<Integer> days;   // 0=Sun..6=Sat
    private String timeStart;     // "HH:mm[:ss]"
    private String timeEnd;       // "HH:mm[:ss]"
}