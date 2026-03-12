package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import com.tcon.learning_management_service.booking.dto.AvailabilityDto;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ClassSessionRepository sessionRepository;
    private final BookingRepository bookingRepository;
    private final TeacherAvailabilityRepository availabilityRepository;

    public List<AvailabilityDto> getTeacherAvailability(String teacherId,
                                                        LocalDateTime start,
                                                        LocalDateTime end) {
        log.info("Getting availability for teacher {} from {} to {}", teacherId, start, end);

        List<AvailabilityDto> availabilityList = new ArrayList<>();

        try {
            Optional<TeacherAvailability> teacherAvailabilityOpt =
                    availabilityRepository.findByTeacherId(teacherId);

            if (teacherAvailabilityOpt.isEmpty()) {
                log.warn("Teacher {} has not configured weekly availability", teacherId);
                return getBookedSessionsOnly(teacherId, start, end);
            }

            TeacherAvailability teacherAvailability = teacherAvailabilityOpt.get();

            LocalDate currentDate = start.toLocalDate();
            LocalDate endDate = end.toLocalDate();

            while (!currentDate.isAfter(endDate)) {
                DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

                List<TimeSlot> daySlots = new ArrayList<>(
                        teacherAvailability.getWeeklyAvailability()
                                .getOrDefault(dayOfWeek, new ArrayList<>())
                );

                // ✅ NEW: if weekly pattern is enabled, add that pattern as a slot
                if (Boolean.TRUE.equals(teacherAvailability.getWeeklyPatternEnabled())) {
                    Integer d1 = teacherAvailability.getWeeklyPatternDay1();
                    Integer d2 = teacherAvailability.getWeeklyPatternDay2();

                    int jsDay = dayOfWeek.getValue() % 7; // JS: 0=Sun, 1=Mon...; Java: 1=Mon..7=Sun
                    if ((d1 != null && jsDay == d1) || (d2 != null && jsDay == d2)) {
                        TimeSlot patternSlot = TimeSlot.builder()
                                .startTime(teacherAvailability.getWeeklyPatternStart()) // "HH:mm"
                                .endTime(teacherAvailability.getWeeklyPatternEnd())
                                .isAvailable(true)
                                .build();
                        daySlots.add(patternSlot);
                    }
                }

                for (TimeSlot slot : daySlots) {
                    LocalTime slotStartTime = LocalTime.parse(slot.getStartTime());
                    LocalTime slotEndTime = LocalTime.parse(slot.getEndTime());
                    LocalDateTime slotStart = LocalDateTime.of(currentDate, slotStartTime);
                    LocalDateTime slotEnd = LocalDateTime.of(currentDate, slotEndTime);

                    if (!slotStart.isBefore(start) && !slotEnd.isAfter(end)) {
                        boolean isBooked = isSlotBooked(teacherId, slotStart, slotEnd);
                        availabilityList.add(AvailabilityDto.builder()
                                .startTime(slotStart)
                                .endTime(slotEnd)
                                .isAvailable(!isBooked)
                                .reason(isBooked ? "Session scheduled" : null)
                                .mode(slot.getMode())
                                .build());
                    }
                }

                currentDate = currentDate.plusDays(1);
            }


            log.info("Generated {} availability slots", availabilityList.size());

        } catch (Exception e) {
            log.error("Error fetching availability: {}", e.getMessage(), e);
            return new ArrayList<>();
        }

        return availabilityList;
    }

    private boolean isSlotBooked(String teacherId, LocalDateTime slotStart, LocalDateTime slotEnd) {
        var bookings = bookingRepository.findByTeacherIdAndSessionStartTimeBetween(
                teacherId, slotStart.minusMinutes(1), slotEnd.plusMinutes(1));

        boolean hasBooking = bookings.stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                        b.getStatus() == BookingStatus.PENDING);

        if (hasBooking) return true;

        var sessions = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                teacherId, slotStart.minusMinutes(1), slotEnd.plusMinutes(1));

        return !sessions.isEmpty();
    }

    private List<AvailabilityDto> getBookedSessionsOnly(String teacherId,
                                                        LocalDateTime start,
                                                        LocalDateTime end) {
        List<AvailabilityDto> bookedSlots = new ArrayList<>();

        var sessions = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                teacherId, start, end);

        sessions.forEach(session -> {
            bookedSlots.add(AvailabilityDto.builder()
                    .startTime(session.getScheduledStartTime())
                    .endTime(session.getScheduledEndTime())
                    .isAvailable(false)
                    .reason("Session scheduled")
                    .build());
        });

        return bookedSlots;
    }
}