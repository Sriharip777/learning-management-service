package com.tcon.learning_management_service.session.dto;


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
public class SessionRescheduleRequest {

    @NotNull(message = "New scheduled start time is required")
    @Future(message = "New scheduled start time must be in the future")
    private LocalDateTime newScheduledStartTime;

    @NotBlank(message = "Reschedule reason is required")
    private String reason;
}
