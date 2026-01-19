package com.tcon.learning_management_service.service;

import com.tcon.learning_management_service.entity.Booking;
import com.tcon.learning_management_service.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final BookingRepository bookingRepository;

    public boolean isTeacherAvailable(String teacherId, LocalDateTime startTime, LocalDateTime endTime) {
        List<Booking> existingBookings = bookingRepository
                .findByTeacherIdAndStartTimeBetween(teacherId, startTime.minusHours(4), endTime.plusHours(4));

        for (Booking booking : existingBookings) {
            if (isOverlapping(booking.getStartTime(), booking.getEndTime(), startTime, endTime)) {
                log.warn("Teacher {} is not available from {} to {}", teacherId, startTime, endTime);
                return false;
            }
        }

        return true;
    }

    private boolean isOverlapping(LocalDateTime start1, LocalDateTime end1,
                                  LocalDateTime start2, LocalDateTime end2) {
        return start1.isBefore(end2) && end1.isAfter(start2);
    }
}
