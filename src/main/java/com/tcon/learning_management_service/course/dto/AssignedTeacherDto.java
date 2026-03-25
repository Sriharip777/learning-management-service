package com.tcon.learning_management_service.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedTeacherDto {

    private String userId;

    private String firstName;
    private String lastName;

    private String avatar;          // profile picture URL

    private Double averageRating;
    private Integer totalReviews;

    private List<String> subjects;
    private List<String> languages;

    // in AssignedTeacherDto
    private Double hourlyRate;

    private String currency;
}