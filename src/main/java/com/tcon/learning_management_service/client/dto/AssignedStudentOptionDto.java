package com.tcon.learning_management_service.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedStudentOptionDto {
    private String userId;
    private String studentId;
    private String name;
    private String email;
}