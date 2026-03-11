package com.tcon.learning_management_service.course.client;

import com.tcon.learning_management_service.course.client.dto.TokenValidationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "auth-user-service",
        url = "${auth.service.url}"
)
public interface AuthServiceClient {

    @GetMapping("/api/auth/validate-token")
    TokenValidationResponse validateToken(
            @RequestHeader("Authorization") String authHeader);
}
