package com.tcon.learning_management_service.booking.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.entity.CancellationPolicy;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
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
    private String teacherName;
    private Boolean isFreeDemo;
    private Integer freeSlotsApplied;
    private Integer paidSlotsApplied;
    private String parentId;
    private String subject;
    private Integer durationMinutes;
    private BookingStatus status;

    private VideoSessionCreateResponse videoSession;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant sessionStartTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant sessionEndTime;

    private List<SessionTimeDto> sessions;

    private BigDecimal amount;
    private String currency;
    private String paymentId;
    private String transactionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant bookedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant confirmedAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant cancelledAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant completedAt;

    private String cancellationReason;
    private String cancelledBy;
    private CancellationPolicy cancellationPolicy;
    private BigDecimal refundAmount;
    private String refundTransactionId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant refundedAt;

    private Boolean reminderSent;
    private String notes;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    private String displaySessionStartTime;
    private String displaySessionEndTime;
    private String displayBookedAt;
    private String displayTimezoneId;
    private String displayTimezoneAbbreviation;

    private String subjectName;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionTimeDto {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant startTime;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private Instant endTime;

        private BigDecimal amount;
    }
}