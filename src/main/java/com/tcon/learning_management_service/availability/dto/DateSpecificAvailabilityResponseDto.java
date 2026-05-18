package com.tcon.learning_management_service.availability.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateSpecificAvailabilityResponseDto {
    private String teacherId;
    private List<DateSpecificAvailabilityDto> dateSlots;
}