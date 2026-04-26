package com.tcon.learning_management_service.client.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoSessionCreateResponse {

    private String id;
    private String bookingId;
    private String classSessionId;
    private String channelName;
    private String status;
    private Boolean canJoin;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime scheduledEndTime;

    private Integer durationMinutes;
    private String teacherId;
    private String studentId;
}