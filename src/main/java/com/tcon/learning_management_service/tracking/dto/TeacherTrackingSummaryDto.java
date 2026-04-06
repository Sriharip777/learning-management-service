package com.tcon.learning_management_service.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherTrackingSummaryDto {
    private Integer totalStudents;
    private Integer onTrackCount;
    private Integer needAttentionCount;
    private Integer avgProgressPercent;
}