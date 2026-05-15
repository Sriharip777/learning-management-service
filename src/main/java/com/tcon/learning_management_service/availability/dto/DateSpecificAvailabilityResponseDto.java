package com.tcon.learning_management_service.availability.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DateSpecificAvailabilityResponseDto {
    private String teacherId;
    private List<DateSpecificAvailabilityDto> dateSlots;
}