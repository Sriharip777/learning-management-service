// src/main/java/com/tcon/learning_management_service/booking/dto/BookingDto.java

package com.tcon.learning_management_service.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.entity.CancellationPolicy;
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
public class BookingDto {
    private String id;
    private String sessionId;
    private String courseId;
    private String studentId;
    private String studentName;
    private String studentEmail;
    private String teacherId;

    private String parentId;
    private String subject;
    private Integer durationMinutes;

    private BookingStatus status;

    // ✅ Single session fields (backward compatibility)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sessionStartTime;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime sessionEndTime;

    // ✅ Multiple sessions (for batch bookings)
    private List<SessionTimeDto> sessions;

    private BigDecimal amount;
    private String currency;
    private String paymentId;
    private String transactionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime bookedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime confirmedAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime cancelledAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime completedAt;

    private String cancellationReason;
    private String cancelledBy;
    private CancellationPolicy cancellationPolicy;
    private BigDecimal refundAmount;
    private String refundTransactionId;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime refundedAt;

    private Boolean reminderSent;
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // ✅ Nested DTO class for multiple sessions (INSIDE BookingDto)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionTimeDto {
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime startTime;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime endTime;

        private BigDecimal amount;
    }
}
