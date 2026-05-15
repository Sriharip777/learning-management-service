package com.tcon.learning_management_service.session.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.entity.SessionParticipant;
import com.tcon.learning_management_service.session.entity.SessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private String id;
    private String courseId;
    private String teacherId;
    private String teacherName;
    private String title;
    private String description;
    private SessionType sessionType;
    private ClassStatus status;



    private Integer durationMinutes;
    private String meetingUrl;
    private String meetingId;
    private String meetingPassword;
    private List<SessionParticipant> participants;
    private Integer maxParticipants;
    private Integer attendedCount;
    private String recordingUrl;
    private List<String> materialUrls;
    private String notes;
    private String rescheduledFromId;
    private String rescheduledToId;
    private String rescheduleReason;

    private String cancellationReason;

    private String cancelledBy;
    private Boolean reminderSent;
    private Instant scheduledStartTime;
    private Instant scheduledEndTime;
    private Instant actualStartTime;
    private Instant actualEndTime;
    private Instant rescheduledAt;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

}
