package com.tcon.learning_management_service.availability.dto;


import com.tcon.learning_management_service.availability.entity.TimeSlot;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateSpecificAvailabilityDto {
    private String date; // "2026-02-15"
    private List<TimeSlot> timeSlots;
}