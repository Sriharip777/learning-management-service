package com.tcon.learning_management_service.client.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoSessionCreateResponse {

    private String id;                    // VideoSession ID in video-service
    private String bookingId;             // Back-reference to booking
    private String classSessionId;        // Back-reference to ClassSession
    private String channelName;           // Agora channel name
    private String status;                // SCHEDULED, IN_PROGRESS, etc.
    private boolean canJoin;              // Computed join window flag
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private String teacherId;
    private String studentId;
}