package com.tcon.learning_management_service.booking.service;


import com.tcon.learning_management_service.booking.dto.AvailabilityDto;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ClassSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;

    public List<AvailabilityDto> getTeacherAvailability(String teacherId,
                                                        LocalDateTime start,
                                                        LocalDateTime end) {
        log.info("Getting availability for teacher {} from {} to {}", teacherId, start, end);

        List<AvailabilityDto> availability = new ArrayList<>();

        // This is a simplified version - in production, you'd check:
        // 1. Teacher's defined availability schedule
        // 2. Existing sessions
        // 3. Existing bookings
        // 4. Buffer times between sessions

        var sessions = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                teacherId, start, end);

        sessions.forEach(session -> {
            availability.add(AvailabilityDto.builder()
                    .startTime(session.getScheduledStartTime())
                    .endTime(session.getScheduledEndTime())
                    .isAvailable(false)
                    .reason("Session scheduled")
                    .build());
        });

        return availability;
    }
}
