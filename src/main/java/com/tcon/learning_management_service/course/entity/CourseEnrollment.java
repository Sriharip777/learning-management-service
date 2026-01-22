package com.tcon.learning_management_service.course.entity;



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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "course_enrollments")
@CompoundIndex(name = "course_student_idx", def = "{'courseId': 1, 'studentId': 1}", unique = true)
public class CourseEnrollment {

    @Id
    private String id;

    @Indexed
    private String courseId;

    @Indexed
    private String studentId;

    private String studentName;
    private String studentEmail;

    private EnrollmentStatus status;

    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
    private LocalDateTime cancelledAt;

    private BigDecimal amountPaid;
    private String paymentId;

    private Integer sessionsAttended;
    private Integer totalSessions;

    private Double progressPercentage;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum EnrollmentStatus {
        PENDING,
        ACTIVE,
        COMPLETED,
        CANCELLED,
        SUSPENDED
    }
}
