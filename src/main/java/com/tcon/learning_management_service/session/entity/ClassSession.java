package com.tcon.learning_management_service.session.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "class_sessions")
public class ClassSession {

    @Id
    private String id;

    @Indexed
    private String courseId;

    @Indexed
    private String teacherId;

    private String teacherName;

    private String title;
    private String description;

    @Builder.Default
    private SessionType sessionType = SessionType.REGULAR;

    @Builder.Default
    private ClassStatus status = ClassStatus.SCHEDULED;

    @Indexed
    private LocalDateTime scheduledStartTime;

    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    private Integer durationMinutes;

    private String meetingUrl;
    private String meetingId;
    private String meetingPassword;

    @Builder.Default
    private List<SessionParticipant> participants = new ArrayList<>();

    private Integer maxParticipants;

    @Builder.Default
    private Integer attendedCount = 0;

    private String recordingUrl;

    @Builder.Default
    private List<String> materialUrls = new ArrayList<>();

    private String notes;

    // Rescheduling
    private String rescheduledFromId;
    private String rescheduledToId;
    private String rescheduleReason;
    private LocalDateTime rescheduledAt;

    // Cancellation
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private String cancelledBy;

    // Reminders
    private Boolean reminderSent;
    private LocalDateTime reminderSentAt;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    private String createdBy;
}
