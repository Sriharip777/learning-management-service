package com.tcon.learning_management_service.client.fallback;

import com.tcon.learning_management_service.client.VideoServiceClient;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateRequest;
import com.tcon.learning_management_service.client.dto.VideoSessionCreateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VideoServiceClientFallback implements VideoServiceClient {

    @Override
    public VideoSessionCreateResponse createVideoSession(VideoSessionCreateRequest request) {
        log.error("Fallback triggered: createVideoSession failed for bookingId={}",
                request != null ? request.getBookingId() : "null");
        return null;
    }

    @Override
    public VideoSessionCreateResponse getSessionByBookingId(String bookingId) {
        log.error("Fallback triggered: getSessionByBookingId failed for bookingId={}", bookingId);
        return null;
    }
}