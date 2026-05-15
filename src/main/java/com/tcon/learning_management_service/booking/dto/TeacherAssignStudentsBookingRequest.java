package com.tcon.learning_management_service.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class TeacherAssignStudentsBookingRequest {

    @NotBlank
    private String teacherId;

    private String teacherName;

    @NotBlank
    private String subject;

    private String notes;

    @NotNull
    @Future
    private Instant sessionStartTime;

    @NotNull
    private Instant sessionEndTime;

    @NotNull
    private BigDecimal amount;

    @Builder.Default
    private String currency = "INR";

    @NotEmpty
    private List<String> studentUserIds;
}