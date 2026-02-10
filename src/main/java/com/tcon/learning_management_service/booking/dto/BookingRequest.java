// src/main/java/com/tcon/learning_management_service/booking/dto/BookingRequest.java

package com.tcon.learning_management_service.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    // ==================== OPTION 1: Session-based booking ====================
    private String sessionId;
    private String courseId;

    // ==================== OPTION 2: Direct teacher booking ====================
    private String teacherId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sessionStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sessionEndTime;

    // ==================== STUDENT INFORMATION (REQUIRED) ====================
    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid email format")
    private String studentEmail;

    // ==================== BOOKING DETAILS ====================
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    private String subject;
    private String notes;
    private String classType;

    private String parentId;  // Optional parent observer
}
