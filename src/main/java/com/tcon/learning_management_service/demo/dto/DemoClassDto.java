package com.tcon.learning_management_service.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcon.learning_management_service.demo.entity.DemoClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemoClassDto {
    private String id;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String teacherId;
    private String teacherName;
    private String courseId;
    private String courseName;
    private DemoClass.DemoStatus status;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant scheduledStartTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant scheduledEndTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant actualStartTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant actualEndTime;

    private Integer durationMinutes;
    private String meetingUrl;
    private String meetingId;
    private String meetingPassword;
    private String studentNotes;
    private String teacherFeedback;
    private Integer studentRating;
    private Integer teacherRating;
    private Boolean convertedToFullCourse;
    private String enrollmentId;
    private Boolean reminderSent;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;
}