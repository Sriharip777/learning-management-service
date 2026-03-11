package com.tcon.learning_management_service.course.client.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenValidationResponse {
    private String userId;
    private String email;
    private String role;
    private boolean valid;
}
