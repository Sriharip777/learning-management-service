package com.tcon.learning_management_service.client;


import com.tcon.learning_management_service.client.dto.VideoSessionCreateRequest;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateResponse;
import com.tcon.learning_management_service.client.fallback.VideoServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "video-service",
        url = "${video.service.url:http://localhost:8083}",
        fallback = VideoServiceClientFallback.class
)
public interface VideoServiceClient {

    // ─────────────────────────────────────────────
    // CREATE VIDEO SESSION
    // Called after booking is confirmed
    // ─────────────────────────────────────────────
    @PostMapping("/api/video/sessions")
    VideoSessionCreateResponse createVideoSession(
            @RequestBody VideoSessionCreateRequest request
    );

    // ─────────────────────────────────────────────
    // GET SESSION BY BOOKING ID
    // Used to check if session already exists
    // ─────────────────────────────────────────────
    @GetMapping("/api/video/sessions/booking/{bookingId}")
    VideoSessionCreateResponse getSessionByBookingId(
            @PathVariable("bookingId") String bookingId
    );
}