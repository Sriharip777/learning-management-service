package com.tcon.learning_management_service.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherTrackingResponseDto {
    private TeacherTrackingSummaryDto summary;
    private List<TeacherTrackingStudentDto> students;
    private List<UpcomingClassDto> upcomingClasses;
}