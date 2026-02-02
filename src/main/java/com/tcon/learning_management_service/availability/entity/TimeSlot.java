package com.tcon.learning_management_service.availability.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {
    private String startTime;  // "10:00:00"
    private String endTime;    // "11:00:00"
    private Boolean isAvailable;
}
