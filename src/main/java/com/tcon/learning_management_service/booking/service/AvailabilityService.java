package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.availability.entity.DateSpecificAvailability;
import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.repository.DateSpecificAvailabilityRepository;
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
import java.time.format.DateTimeFormatter;
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
    private final DateSpecificAvailabilityRepository dateSpecificAvailabilityRepository;

    /**
     * Main entry: used by /api/bookings/availability/teacher/{teacherId}
     */
    public List<AvailabilityDto> getTeacherAvailability(String teacherId,
                                                        LocalDateTime start,
                                                        LocalDateTime end) {
        log.info("Getting availability for teacher {} from {} to {}", teacherId, start, end);

        List<AvailabilityDto> availabilityList = new ArrayList<>();

        try {
            // ─── Weekly + weekly pattern ───────────────────────────────
            Optional<TeacherAvailability> teacherAvailabilityOpt =
                    availabilityRepository.findByTeacherId(teacherId);

            if (teacherAvailabilityOpt.isEmpty()) {
                log.warn("Teacher {} has not configured weekly availability", teacherId);
                // do NOT return; we still want date-specific availability
            } else {
                TeacherAvailability teacherAvailability = teacherAvailabilityOpt.get();

                LocalDate currentDate = start.toLocalDate();
                LocalDate endDate = end.toLocalDate();

                while (!currentDate.isAfter(endDate)) {
                    DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

                    List<TimeSlot> daySlots = new ArrayList<>(
                            teacherAvailability.getWeeklyAvailability()
                                    .getOrDefault(dayOfWeek, new ArrayList<>())
                    );

                    // Weekly pattern support (monthly UI config)
                    if (Boolean.TRUE.equals(teacherAvailability.getWeeklyPatternEnabled())
                            && teacherAvailability.getWeeklyPatternDays() != null
                            && !teacherAvailability.getWeeklyPatternDays().isEmpty()) {

                        // JS-style day (0=Sunday ... 6=Saturday)
                        int jsDay = dayOfWeek.getValue() % 7;

                        if (teacherAvailability.getWeeklyPatternDays().contains(jsDay)) {
                            TimeSlot patternSlot = TimeSlot.builder()
                                    .startTime(teacherAvailability.getWeeklyPatternStart()) // "HH:mm"
                                    .endTime(teacherAvailability.getWeeklyPatternEnd())
                                    .isAvailable(true)
                                    .mode(null) // no specific mode; frontend filters if needed
                                    .build();
                            daySlots.add(patternSlot);
                        }
                    }

                    // Convert weekly slots to AvailabilityDto
                    for (TimeSlot slot : daySlots) {
                        try {
                            LocalTime slotStartTime = LocalTime.parse(slot.getStartTime());
                            LocalTime slotEndTime   = LocalTime.parse(slot.getEndTime());
                            LocalDateTime slotStart = LocalDateTime.of(currentDate, slotStartTime);
                            LocalDateTime slotEnd   = LocalDateTime.of(currentDate, slotEndTime);

                            // Respect requested window
                            if (!slotStart.isBefore(start) && !slotEnd.isAfter(end)) {
                                boolean isBooked = isSlotBooked(teacherId, slotStart, slotEnd);
                                availabilityList.add(
                                        AvailabilityDto.builder()
                                                .startTime(slotStart)
                                                .endTime(slotEnd)
                                                .isAvailable(!isBooked && Boolean.TRUE.equals(slot.getIsAvailable()))
                                                .reason(isBooked ? "Session scheduled" : null)
                                                .mode(slot.getMode())
                                                .build()
                                );
                            }
                        } catch (Exception ex) {
                            log.error("Failed to process weekly slot {} - {} on {}: {}",
                                    slot.getStartTime(), slot.getEndTime(), currentDate, ex.getMessage());
                        }
                    }

                    currentDate = currentDate.plusDays(1);
                }
            }

            // ─── Date-specific availability ─────────────────────────────
            List<AvailabilityDto> dateSpecific = getDateSpecificAvailability(teacherId, start, end);
            availabilityList.addAll(dateSpecific);

            log.info("Generated {} availability slots (weekly + date-specific) for teacher {}",
                    availabilityList.size(), teacherId);

        } catch (Exception e) {
            log.error("Error fetching availability: {}", e.getMessage(), e);
            return new ArrayList<>();
        }

        return availabilityList;
    }

    /**
     * Check if a given slot window is already booked or has a scheduled class.
     */
    private boolean isSlotBooked(String teacherId, LocalDateTime slotStart, LocalDateTime slotEnd) {
        var bookings = bookingRepository.findByTeacherIdAndSessionStartTimeBetween(
                teacherId, slotStart.minusMinutes(1), slotEnd.plusMinutes(1));

        boolean hasBooking = bookings.stream()
                .anyMatch(b -> b.getStatus() == BookingStatus.CONFIRMED ||
                        b.getStatus() == BookingStatus.PENDING);

        if (hasBooking) {
            return true;
        }

        var sessions = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                teacherId, slotStart.minusMinutes(1), slotEnd.plusMinutes(1));

        return !sessions.isEmpty();
    }

    /**
     * Convert DateSpecificAvailability rows to AvailabilityDto in the requested window.
     */
    private List<AvailabilityDto> getDateSpecificAvailability(
            String teacherId,
            LocalDateTime start,
            LocalDateTime end) {

        List<AvailabilityDto> result = new ArrayList<>();

        LocalDate fromDate = start.toLocalDate();
        LocalDate toDate   = end.toLocalDate();

        log.info("🔍 Date-specific lookup for teacher {} between {} and {} (LocalDate)",
                teacherId, fromDate, toDate);

        // Widen by 1 day on each side to avoid off-by-one due to time vs date
        var entries = dateSpecificAvailabilityRepository
                .findByTeacherIdAndDateBetween(
                        teacherId,
                        fromDate.minusDays(1),
                        toDate.plusDays(1)
                );

        log.info("📊 Raw DateSpecificAvailability entries: {}", entries.size());
        entries.forEach(e ->
                log.info("  - entry id={}, date={}", e.getId(), e.getDate())
        );

        DateTimeFormatter parseTimeFormatter =
                DateTimeFormatter.ofPattern("HH:mm[:ss]");

        for (DateSpecificAvailability avail : entries) {
            LocalDate date = avail.getDate();

            avail.getTimeSlots().forEach(slot -> {
                try {
                    LocalTime startTime = LocalTime.parse(slot.getStartTime(), parseTimeFormatter);
                    LocalTime endTime   = LocalTime.parse(slot.getEndTime(),   parseTimeFormatter);

                    LocalDateTime slotStart = LocalDateTime.of(date, startTime);
                    LocalDateTime slotEnd   = LocalDateTime.of(date, endTime);

                    // Respect requested [start, end] window
                    if (slotEnd.isBefore(start) || slotStart.isAfter(end)) {
                        return;
                    }

                    boolean isBooked = isSlotBooked(teacherId, slotStart, slotEnd);

                    result.add(
                            AvailabilityDto.builder()
                                    .startTime(slotStart)
                                    .endTime(slotEnd)
                                    .isAvailable(!isBooked && Boolean.TRUE.equals(slot.getIsAvailable()))
                                    .reason(isBooked ? "Session scheduled" : null)
                                    .mode(slot.getMode())
                                    .build()
                    );
                } catch (Exception ex) {
                    log.error("Failed to process date-specific slot {} - {} on {}: {}",
                            slot.getStartTime(), slot.getEndTime(), date, ex.getMessage());
                }
            });
        }

        log.info("Loaded {} date-specific availability slots for teacher {}", result.size(), teacherId);
        return result;
    }

    // (Optional: keep if you ever need purely booked sessions)
    @SuppressWarnings("unused")
    private List<AvailabilityDto> getBookedSessionsOnly(String teacherId,
                                                        LocalDateTime start,
                                                        LocalDateTime end) {
        List<AvailabilityDto> bookedSlots = new ArrayList<>();

        var sessions = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(
                teacherId, start, end);

        sessions.forEach(session -> bookedSlots.add(
                AvailabilityDto.builder()
                        .startTime(session.getScheduledStartTime())
                        .endTime(session.getScheduledEndTime())
                        .isAvailable(false)
                        .reason("Session scheduled")
                        .build()
        ));

        return bookedSlots;
    }
}