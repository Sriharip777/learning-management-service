package com.tcon.learning_management_service.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentBookingAnalyticsDto {
    private String studentId;
    private Integer totalClasses;
    private Integer completedClasses;
    private Integer cancelledClasses;
    private Integer totalMinutesLearned;
}