package com.tcon.learning_management_service.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlotDto {
    private Instant startTimeUtc;
    private Instant endTimeUtc;
    private Boolean isAvailable;
    private SessionMode mode;
}