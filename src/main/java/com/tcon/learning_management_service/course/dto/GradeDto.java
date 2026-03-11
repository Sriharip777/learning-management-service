package com.tcon.learning_management_service.course.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GradeDto {
    private String id;
    @NotBlank
    private String name;
    private Integer order;
    private Boolean isActive;
}
