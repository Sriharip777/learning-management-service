package com.tcon.learning_management_service.availability.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotDto {
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isAvailable;
}
