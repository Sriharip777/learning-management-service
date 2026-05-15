package com.tcon.learning_management_service.availability.entity;

import com.tcon.learning_management_service.availability.dto.SessionMode;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyTimeSlot {
    // Local time pattern as string, "HH:mm" or "HH:mm:ss"
    private String startTime;
    private String endTime;
    private Boolean isAvailable;
    private SessionMode mode;
}