package com.tcon.learning_management_service.booking.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchBookingRequest {

    @NotBlank(message = "Teacher ID is required")
    private String teacherId;

    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid email format")
    private String studentEmail;

    @NotEmpty(message = "At least one session is required")
    private List<SessionSlot> sessions;

    private String courseId;
    private String notes;

    @Builder.Default
    private String currency = "INR";

    @Positive(message = "Total amount must be positive")
    private BigDecimal totalAmount;

    // ✅ Nested class for individual session slots
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSlot {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime sessionStartTime;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime sessionEndTime;

        @Positive(message = "Amount must be positive")
        private BigDecimal amount;
    }
}
