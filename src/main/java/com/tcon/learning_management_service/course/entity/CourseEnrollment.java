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
import java.time.Instant;

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

    private Instant enrolledAt;
    private Instant completedAt;
    private Instant cancelledAt;

    private BigDecimal amountPaid;
    private String paymentId;

    private Integer sessionsAttended;
    private Integer totalSessions;

    private Double progressPercentage;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;

    private String sessionMode;

    public enum EnrollmentStatus {
        PENDING,
        ACTIVE,
        COMPLETED,
        CANCELLED,
        SUSPENDED
    }
}