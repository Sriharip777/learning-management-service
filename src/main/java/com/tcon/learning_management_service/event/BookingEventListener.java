package com.tcon.learning_management_service.event;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    @KafkaListener(topics = "booking-events", groupId = "learning-management-service")
    public void handleBookingEvent(BookingEventPublisher.BookingEvent event) {
        log.info("Received booking event: {} for booking: {}", event.getEventType(), event.getBookingId());

        switch (event.getEventType()) {
            case "BOOKING_CREATED":
                handleBookingCreated(event);
                break;
            case "BOOKING_CONFIRMED":
                handleBookingConfirmed(event);
                break;
            case "BOOKING_CANCELLED":
                handleBookingCancelled(event);
                break;
            default:
                log.warn("Unknown booking event type: {}", event.getEventType());
        }
    }

    private void handleBookingCreated(BookingEventPublisher.BookingEvent event) {
        log.info("Processing booking created: {}", event.getBookingId());
        // Add logic: Send notification to teacher
        // Add logic: Update analytics
    }

    private void handleBookingConfirmed(BookingEventPublisher.BookingEvent event) {
        log.info("Processing booking confirmed: {}", event.getBookingId());
        // Add logic: Send confirmation email
        // Add logic: Add to calendar
        // Add logic: Send meeting details
    }

    private void handleBookingCancelled(BookingEventPublisher.BookingEvent event) {
        log.info("Processing booking cancelled: {}", event.getBookingId());
        // Add logic: Send cancellation notification
        // Add logic: Process refund
        // Add logic: Update teacher availability
    }
}
