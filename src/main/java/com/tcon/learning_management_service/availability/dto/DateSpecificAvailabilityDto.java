package com.tcon.learning_management_service.availability.dto;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateSpecificAvailabilityDto {
    // Start of the UTC day this set of slots belongs to (e.g. 2026-05-15T00:00:00Z)
    private Instant dayStartUtc;
    private List<AvailabilitySlotDto> slots;
}