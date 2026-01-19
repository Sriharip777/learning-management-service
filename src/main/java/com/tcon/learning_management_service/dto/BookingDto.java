package com.tcon.learning_management_service.dto;

import com.tcon.learning_management_service.entity.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDto {
    private String id;
    private String courseId;
    private String studentId;
    private String teacherId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BookingStatus status;
    private Double amount;
    private String currency;
    private String paymentId;
    private LocalDateTime createdAt;
}
