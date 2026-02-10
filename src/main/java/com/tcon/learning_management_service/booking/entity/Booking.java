package com.tcon.learning_management_service.booking.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "bookings")
@CompoundIndex(name = "session_student_idx", def = "{'sessionId': 1, 'studentId': 1}", unique = false)
public class Booking {

    @Id
    private String id;

    @Indexed
    private String sessionId;

    @Indexed
    private String courseId;

    @Indexed
    private String studentId;

    private String studentName;
    private String studentEmail;

    @Indexed
    private String teacherId;


    private String parentId;           // Optional parent observer
    private String subject;            // What the class is about
    private Integer durationMinutes;   // How long the class is


    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    // ✅ Single session fields (backward compatibility)
    private LocalDateTime sessionStartTime;
    private LocalDateTime sessionEndTime;

    // ✅ Multiple sessions (for batch bookings)
    private List<SessionTime> sessions;

    private BigDecimal amount;
    private String currency;

    private String paymentId;
    private String transactionId;

    private LocalDateTime bookedAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime cancelledAt;
    private LocalDateTime completedAt;

    private String cancellationReason;
    private String cancelledBy;

    private CancellationPolicy cancellationPolicy;

    private BigDecimal refundAmount;
    private String refundTransactionId;
    private LocalDateTime refundedAt;

    private Boolean reminderSent;
    private LocalDateTime reminderSentAt;

    private String notes;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // ✅ ADD THIS: Nested class for multiple sessions
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionTime {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private BigDecimal amount;
    }
}
