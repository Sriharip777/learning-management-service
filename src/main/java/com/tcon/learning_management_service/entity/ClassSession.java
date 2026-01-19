package com.tcon.learning_management_service.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

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
    private String bookingId;

    @Indexed
    private String teacherId;

    @Indexed
    private String studentId;

    @Indexed
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;

    private LocalDateTime actualStartTime;
    private LocalDateTime actualEndTime;

    private ClassStatus status;

    private String videoRoomId;
    private String recordingUrl;

    private Boolean reminderSent;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
