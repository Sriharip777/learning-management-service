package com.tcon.learning_management_service.booking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherBookingAnalyticsDto {
    private String teacherId;
    private Integer totalClasses;
    private Integer completedClasses;
    private Integer cancelledClasses;
    private Integer uniqueStudents;
}