package com.tcon.learning_management_service.booking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAvailable;
    private String reason;
}
