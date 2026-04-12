package com.tcon.learning_management_service.client.dto;


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

    // ── Linking fields ──────────────────────────
    private String bookingId;         // Links video session to booking
    private String classSessionId;    // Links to ClassSession entity

    // ── Participants ─────────────────────────────
    private String teacherId;
    private String studentId;

    // ── Session details ──────────────────────────
    private String subject;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;

    // ── Agora config ─────────────────────────────
    private String channelName;       // Unique Agora channel per booking
    private boolean recordingEnabled; // Always true for paid/demo sessions
}