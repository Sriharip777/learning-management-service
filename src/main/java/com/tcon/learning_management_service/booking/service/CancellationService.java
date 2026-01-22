package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.event.BookingEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationService {

    private final BookingRepository bookingRepository;
    private final BookingEventPublisher eventPublisher;

    @Transactional
    public BigDecimal cancelBooking(String bookingId, String userId, String reason) {
        log.info("Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        // Validate ownership
        if (!booking.getStudentId().equals(userId) && !booking.getTeacherId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized: User does not own this booking");
        }

        // Validate status
        if (booking.getStatus() != BookingStatus.CONFIRMED && booking.getStatus() != BookingStatus.PENDING) {
            throw new IllegalArgumentException("Only confirmed or pending bookings can be cancelled");
        }

        // Check if session has already started
        if (booking.getSessionStartTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot cancel bookings for sessions that have already started");
        }

        // Calculate refund amount
        BigDecimal refundAmount = calculateRefundAmount(booking);

        // Update booking
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        booking.setCancelledAt(LocalDateTime.now());
        booking.setCancelledBy(userId);
        booking.setRefundAmount(refundAmount);

        Booking updated = bookingRepository.save(booking);
        log.info("Booking cancelled. Refund amount: {}", refundAmount);

        // Publish event
        eventPublisher.publishBookingCancelled(updated);

        return refundAmount;
    }

    private BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getAmount() == null || booking.getCancellationPolicy() == null) {
            return BigDecimal.ZERO;
        }

        LocalDateTime now = LocalDateTime.now();
        long hoursUntilSession = Duration.between(now, booking.getSessionStartTime()).toHours();

        // Check cancellation policy
        if (hoursUntilSession >= booking.getCancellationPolicy().getHoursBeforeSession()) {
            // Full refund or policy refund percentage
            BigDecimal refundPercentage = BigDecimal.valueOf(
                    booking.getCancellationPolicy().getRefundPercentage());
            return booking.getAmount()
                    .multiply(refundPercentage)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else if (hoursUntilSession >= 12) {
            // 50% refund if cancelled 12-24 hours before
            return booking.getAmount()
                    .multiply(BigDecimal.valueOf(0.5))
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (hoursUntilSession >= 6) {
            // 25% refund if cancelled 6-12 hours before
            return booking.getAmount()
                    .multiply(BigDecimal.valueOf(0.25))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            // No refund if cancelled less than 6 hours before
            return BigDecimal.ZERO;
        }
    }
}
