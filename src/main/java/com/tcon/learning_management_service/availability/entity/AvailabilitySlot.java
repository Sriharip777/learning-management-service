package com.tcon.learning_management_service.availability.entity;

import com.tcon.learning_management_service.availability.dto.SessionMode;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilitySlot {
    private Instant startTimeUtc;
    private Instant endTimeUtc;
    private Boolean isAvailable;
    private SessionMode mode;
}