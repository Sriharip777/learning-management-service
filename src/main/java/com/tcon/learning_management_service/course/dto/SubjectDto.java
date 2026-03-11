package com.tcon.learning_management_service.course.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectDto {
    private String id;
    @NotBlank
    private String gradeId;
    @NotBlank
    private String name;
    private String description;
    private Boolean isActive;
}
