package com.tcon.learning_management_service.demo.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "demo_classes")
@CompoundIndex(name = "student_teacher_idx", def = "{'studentId': 1, 'teacherId': 1}")
public class DemoClass {

    @Id
    private String id;

    @Indexed
    private String studentId;

    private String studentName;
    private String studentEmail;

    @Indexed
    private String teacherId;

    private String teacherName;

    @Indexed
    private String courseId;

    private String courseName;

    private DemoStatus status;

    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualStartTime;
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
    private LocalDateTime reminderSentAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum DemoStatus {
        SCHEDULED,
        CONFIRMED,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED,
        NO_SHOW
    }
}
