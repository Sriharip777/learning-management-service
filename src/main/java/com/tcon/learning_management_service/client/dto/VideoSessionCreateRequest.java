package com.tcon.learning_management_service.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoSessionCreateRequest {

    private String bookingId;
    private String classSessionId;

    private String teacherId;
    private String studentId;
    private String parentId;

    private String subject;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    private Integer durationMinutes;

    private String channelName;

    @Builder.Default
    private Boolean recordingEnabled = true;

    @Builder.Default
    private Boolean whiteboardEnabled = true;

    @Builder.Default
    private Boolean chatEnabled = true;
}