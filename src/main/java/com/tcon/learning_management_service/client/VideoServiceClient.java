package com.tcon.learning_management_service.client;

import com.tcon.learning_management_service.client.dto.VideoSessionCreateRequest;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateResponse;
import com.tcon.learning_management_service.client.fallback.VideoServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "video-service",
        url = "${video.service.url}",
        fallback = VideoServiceClientFallback.class
)
public interface VideoServiceClient {

    @PostMapping("/api/video/sessions")
    VideoSessionCreateResponse createVideoSession(
            @RequestBody VideoSessionCreateRequest request
    );

    @GetMapping("/api/video/sessions/booking/{bookingId}")
    VideoSessionCreateResponse getSessionByBookingId(
            @PathVariable("bookingId") String bookingId
    );
}