package com.tcon.learning_management_service.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
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

    private String sessionId;
    private String courseId;

    private String teacherId;
    private String teacherName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sessionStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sessionEndTime;

    @NotBlank(message = "Student name is required")
    private String studentName;

    @NotBlank(message = "Student email is required")
    @Email(message = "Invalid email format")
    private String studentEmail;

    private Boolean isFreeDemo;

    @PositiveOrZero(message = "Amount must be zero or positive")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    private String subject;
    private String notes;
    private String classType;
    private String parentId;

    @AssertTrue(message = "Amount must be zero or positive")
    public boolean isAmountValid() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) >= 0;
    }
}