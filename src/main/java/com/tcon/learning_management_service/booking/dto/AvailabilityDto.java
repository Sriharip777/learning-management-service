package com.tcon.learning_management_service.booking.dto;

import com.tcon.learning_management_service.availability.dto.SessionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityDto {
    private Instant startTime;
    private Instant endTime;
    private Boolean isAvailable;
    private String reason;
    private SessionMode mode;
}