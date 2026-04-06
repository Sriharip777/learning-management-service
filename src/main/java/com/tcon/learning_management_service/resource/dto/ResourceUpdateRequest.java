package com.tcon.learning_management_service.resource.dto;


import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceUpdateRequest {
    private String title;
    private String description;
    private Boolean isActive;
}