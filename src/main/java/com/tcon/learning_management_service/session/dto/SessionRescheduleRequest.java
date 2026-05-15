package com.tcon.learning_management_service.session.dto;


import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionRescheduleRequest {

    // SessionRescheduleRequest
    private Instant newScheduledStartTime;

    @NotBlank(message = "Reschedule reason is required")
    private String reason;
}
