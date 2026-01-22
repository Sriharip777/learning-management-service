package com.tcon.learning_management_service.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "calendar-events";

    public void publishAddToCalendar(String userId, String eventTitle,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     String description, String meetingUrl) {
        try {
            CalendarEvent event = CalendarEvent.builder()
                    .eventType("ADD_TO_CALENDAR")
                    .userId(userId)
                    .eventTitle(eventTitle)
                    .startTime(startTime)
                    .endTime(endTime)
                    .description(description)
                    .meetingUrl(meetingUrl)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, userId, event);
            log.info("Published add to calendar event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish calendar event", e);
        }
    }

    public void publishRemoveFromCalendar(String userId, String eventId) {
        try {
            CalendarEvent event = CalendarEvent.builder()
                    .eventType("REMOVE_FROM_CALENDAR")
                    .userId(userId)
                    .eventId(eventId)
                    .timestamp(LocalDateTime.now())
                    .build();

            kafkaTemplate.send(TOPIC, userId, event);
            log.info("Published remove from calendar event for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to publish calendar removal event", e);
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CalendarEvent {
        private String eventType;
        private String userId;
        private String eventId;
        private String eventTitle;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String description;
        private String meetingUrl;
        private LocalDateTime timestamp;
    }
}
