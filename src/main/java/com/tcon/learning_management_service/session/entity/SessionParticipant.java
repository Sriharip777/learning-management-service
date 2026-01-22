package com.tcon.learning_management_service.session.entity;



import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionParticipant {
    private String studentId;
    private String studentName;
    private String studentEmail;
    private Boolean attended;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
    private Integer durationMinutes;
    private String feedback;
    private Integer rating;
}
