package com.tcon.learning_management_service.course.dto;

import lombok.*;
import jakarta.validation.constraints.NotBlank;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicDto {
    private String id;
    @NotBlank
    private String subjectId;
    @NotBlank
    private String name;
    private String description;
    private Boolean isActive;
}
