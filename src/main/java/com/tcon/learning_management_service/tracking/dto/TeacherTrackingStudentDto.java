package com.tcon.learning_management_service.tracking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherTrackingStudentDto {
    private String id;
    private String studentId;
    private String name;
    private String email;
    private String course;
    private Integer hoursAttended;
    private Integer totalHours;
    private Integer attendance;
    private Integer homeworkCompleted;
    private Integer totalHomework;
    private Integer worksheetScore;
    private Integer classesRemaining;
    private String status; // excellent | on_track | needs_attention
    private String lastActive;
}