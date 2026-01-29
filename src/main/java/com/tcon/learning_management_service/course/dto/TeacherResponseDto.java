
package com.tcon.learning_management_service.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherResponseDto {
    private String id;
    private String userId;
    private String bio;
    private List<String> subjects;
    private List<String> languages;
    private Integer yearsOfExperience;
    private String qualifications;
    private Double hourlyRate;
    private Double averageRating;
    private Integer totalReviews;
    private String verificationStatus;
    private Boolean isAvailable;
    private String timezone;
}
