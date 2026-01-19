package com.tcon.learning_management_service.dto;

import com.tcon.learning_management_service.entity.ClassStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private String id;
    private String courseId;
    private String bookingId;
    private String teacherId;
    private String studentId;
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;
    private ClassStatus status;
    private String videoRoomId;
    private String recordingUrl;
    private LocalDateTime createdAt;
}
