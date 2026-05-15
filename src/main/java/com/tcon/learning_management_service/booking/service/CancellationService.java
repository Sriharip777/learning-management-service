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
import java.time.Instant;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal FIFTY_PERCENT = new BigDecimal("0.50");
    private static final BigDecimal TWENTY_FIVE_PERCENT = new BigDecimal("0.25");

    private final BookingRepository bookingRepository;
    private final BookingEventPublisher eventPublisher;

    @Transactional
    public BigDecimal cancelBooking(String bookingId, String userId, String reason) {
        log.info("Cancelling booking: {} by user: {}", bookingId, userId);

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

        validateOwnership(booking, userId);
        validateCancellableStatus(booking);
        validateSessionNotStarted(booking);

        BigDecimal refundAmount = calculateRefundAmount(booking);
        Instant now = Instant.now();

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reasonOrDefault(reason));
        booking.setCancelledAt(now);
        booking.setCancelledBy(userId);
        booking.setRefundAmount(refundAmount);
        booking.setUpdatedAt(now);

        Booking updated = bookingRepository.save(booking);
        log.info("Booking cancelled successfully. bookingId={}, refundAmount={}", bookingId, refundAmount);

        eventPublisher.publishBookingCancelled(updated);

        return refundAmount;
    }

    private void validateOwnership(Booking booking, String userId) {
        boolean isStudent = Objects.equals(booking.getStudentId(), userId);
        boolean isTeacher = Objects.equals(booking.getTeacherId(), userId);

        if (!isStudent && !isTeacher) {
            throw new IllegalArgumentException("Unauthorized: User does not own this booking");
        }
    }

    private void validateCancellableStatus(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED
                && booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new IllegalArgumentException("Only confirmed or pending bookings can be cancelled");
        }
    }

    private void validateSessionNotStarted(Booking booking) {
        if (booking.getSessionStartTime() == null) {
            throw new IllegalArgumentException("Booking session start time is missing");
        }

        if (!booking.getSessionStartTime().isAfter(Instant.now())) {
            throw new IllegalArgumentException("Cannot cancel bookings for sessions that have already started");
        }
    }

    private BigDecimal calculateRefundAmount(Booking booking) {
        if (booking.getAmount() == null
                || booking.getAmount().compareTo(BigDecimal.ZERO) <= 0
                || booking.getCancellationPolicy() == null
                || booking.getSessionStartTime() == null) {
            return BigDecimal.ZERO;
        }

        Instant now = Instant.now();
        long hoursUntilSession = Duration.between(now, booking.getSessionStartTime()).toHours();

        if (hoursUntilSession >= booking.getCancellationPolicy().getHoursBeforeSession()) {
            BigDecimal refundPercentage = toBigDecimal(booking.getCancellationPolicy().getRefundPercentage());

            return booking.getAmount()
                    .multiply(refundPercentage)
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        } else if (hoursUntilSession >= 12) {
            return booking.getAmount()
                    .multiply(FIFTY_PERCENT)
                    .setScale(2, RoundingMode.HALF_UP);
        } else if (hoursUntilSession >= 6) {
            return booking.getAmount()
                    .multiply(TWENTY_FIVE_PERCENT)
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }
        if (value instanceof Integer integer) {
            return BigDecimal.valueOf(integer.longValue());
        }
        if (value instanceof Long longValue) {
            return BigDecimal.valueOf(longValue);
        }
        if (value instanceof Double doubleValue) {
            return BigDecimal.valueOf(doubleValue);
        }
        if (value instanceof String stringValue) {
            return new BigDecimal(stringValue.trim());
        }
        throw new IllegalArgumentException("Unsupported refund percentage type: " + value.getClass().getName());
    }

    private String reasonOrDefault(String reason) {
        return (reason != null && !reason.isBlank()) ? reason.trim() : "Cancelled by user";
    }
}