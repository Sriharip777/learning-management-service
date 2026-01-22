package com.tcon.learning_management_service.demo.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcon.learning_management_service.demo.entity.DemoClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime actualEndTime;

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

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
