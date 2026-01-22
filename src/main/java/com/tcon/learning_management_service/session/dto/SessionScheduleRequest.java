package com.tcon.learning_management_service.session.dto;


import com.tcon.learning_management_service.session.entity.SessionType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionScheduleRequest {

    @NotBlank(message = "Course ID is required")
    private String courseId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Builder.Default
    private SessionType sessionType = SessionType.REGULAR;

    @NotNull(message = "Scheduled start time is required")
    @Future(message = "Scheduled start time must be in the future")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "Duration is required")
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    private Integer durationMinutes;

    private String meetingUrl;
    private String meetingId;
    private String meetingPassword;

    private Integer maxParticipants;
    private List<String> materialUrls;
    private String notes;
}

