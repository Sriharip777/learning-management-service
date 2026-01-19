package com.tcon.learning_management_service.dto;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionScheduleRequest {

    @NotBlank(message = "Course ID is required")
    private String courseId;

    @NotBlank(message = "Booking ID is required")
    private String bookingId;

    @NotBlank(message = "Teacher ID is required")
    private String teacherId;

    @NotBlank(message = "Student ID is required")
    private String studentId;

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "End time is required")
    private LocalDateTime scheduledEndTime;
}
