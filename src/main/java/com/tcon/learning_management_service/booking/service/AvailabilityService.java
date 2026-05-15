package com.tcon.learning_management_service.booking.service;

import com.tcon.learning_management_service.availability.entity.AvailabilitySlot;
import com.tcon.learning_management_service.availability.entity.DateSpecificAvailability;
import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.entity.WeeklyTimeSlot;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
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

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm[:ss]");

    public List<AvailabilityDto> getTeacherAvailability(String teacherId, Instant start, Instant end) {
        log.info("Getting availability for teacher {} from {} to {}", teacherId, start, end);

        if (teacherId == null || teacherId.isBlank()) {
            throw new IllegalArgumentException("Teacher ID is required");
        }
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end instants are required");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End must be after start");
        }

        List<AvailabilityDto> availabilityList = new ArrayList<>();

        try {
            Optional<TeacherAvailability> teacherAvailabilityOpt = availabilityRepository.findByTeacherId(teacherId);

            if (teacherAvailabilityOpt.isEmpty()) {
                log.warn("Teacher {} has not configured weekly availability", teacherId);
            } else {
                TeacherAvailability teacherAvailability = teacherAvailabilityOpt.get();
                availabilityList.addAll(buildWeeklyAvailability(teacherId, teacherAvailability, start, end));
            }

            availabilityList.addAll(getDateSpecificAvailability(teacherId, start, end));
            availabilityList.sort(Comparator.comparing(AvailabilityDto::getStartTime));

            log.info("Generated {} availability slots for teacher {}", availabilityList.size(), teacherId);
            return availabilityList;
        } catch (Exception e) {
            log.error("Error fetching availability for teacher {}: {}", teacherId, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private List<AvailabilityDto> buildWeeklyAvailability(
            String teacherId,
            TeacherAvailability teacherAvailability,
            Instant start,
            Instant end
    ) {
        List<AvailabilityDto> result = new ArrayList<>();

        LocalDate currentDate = start.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = end.atZone(ZoneOffset.UTC).toLocalDate();

        while (!currentDate.isAfter(endDate)) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();

            List<WeeklyTimeSlot> daySlots = new ArrayList<>(
                    teacherAvailability.getWeeklyAvailability() != null
                            ? teacherAvailability.getWeeklyAvailability().getOrDefault(dayOfWeek, List.of())
                            : List.of()
            );

            if (Boolean.TRUE.equals(teacherAvailability.getWeeklyPatternEnabled())
                    && teacherAvailability.getWeeklyPatternDays() != null
                    && !teacherAvailability.getWeeklyPatternDays().isEmpty()
                    && teacherAvailability.getWeeklyPatternStart() != null
                    && teacherAvailability.getWeeklyPatternEnd() != null) {

                int jsDay = dayOfWeek.getValue() % 7;

                if (teacherAvailability.getWeeklyPatternDays().contains(jsDay)) {
                    daySlots.add(
                            WeeklyTimeSlot.builder()
                                    .startTime(teacherAvailability.getWeeklyPatternStart())
                                    .endTime(teacherAvailability.getWeeklyPatternEnd())
                                    .isAvailable(true)
                                    .mode(null)
                                    .build()
                    );
                }
            }

            for (WeeklyTimeSlot slot : daySlots) {
                AvailabilityDto dto = buildWeeklyAvailabilityFromSlot(teacherId, currentDate, slot, start, end);
                if (dto != null) {
                    result.add(dto);
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        return result;
    }

    private AvailabilityDto buildWeeklyAvailabilityFromSlot(
            String teacherId,
            LocalDate date,
            WeeklyTimeSlot slot,
            Instant rangeStart,
            Instant rangeEnd
    ) {
        try {
            if (slot == null || slot.getStartTime() == null || slot.getEndTime() == null) {
                return null;
            }

            LocalTime slotStartTime = parseTime(slot.getStartTime());
            LocalTime slotEndTime = parseTime(slot.getEndTime());

            Instant slotStart = date.atTime(slotStartTime).toInstant(ZoneOffset.UTC);
            Instant slotEnd = date.atTime(slotEndTime).toInstant(ZoneOffset.UTC);

            if (!slotEnd.isAfter(slotStart)) {
                log.warn("Skipping invalid weekly slot on {}: {} - {}", date, slot.getStartTime(), slot.getEndTime());
                return null;
            }

            if (slotEnd.isBefore(rangeStart) || slotStart.isAfter(rangeEnd)) {
                return null;
            }

            boolean isBooked = isSlotBooked(teacherId, slotStart, slotEnd);

            return AvailabilityDto.builder()
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .isAvailable(!isBooked && Boolean.TRUE.equals(slot.getIsAvailable()))
                    .reason(isBooked ? "Session scheduled" : null)
                    .mode(slot.getMode())
                    .build();
        } catch (Exception ex) {
            log.error("Failed to process weekly slot {} - {} on {}: {}",
                    slot.getStartTime(),
                    slot.getEndTime(),
                    date,
                    ex.getMessage(),
                    ex);
            return null;
        }
    }

    private List<AvailabilityDto> getDateSpecificAvailability(String teacherId, Instant start, Instant end) {
        List<AvailabilityDto> result = new ArrayList<>();

        log.info("Date-specific lookup for teacher {} between {} and {}", teacherId, start, end);

        List<DateSpecificAvailability> entries = loadDateSpecificEntries(teacherId);

        for (DateSpecificAvailability avail : entries) {
            if (avail == null || avail.getSlots() == null || avail.getSlots().isEmpty()) {
                continue;
            }

            for (AvailabilitySlot slot : avail.getSlots()) {
                AvailabilityDto dto = buildDateSpecificAvailabilityFromSlot(teacherId, slot, start, end);
                if (dto != null) {
                    result.add(dto);
                }
            }
        }

        log.info("Loaded {} date-specific availability slots for teacher {}", result.size(), teacherId);
        return result;
    }

    private AvailabilityDto buildDateSpecificAvailabilityFromSlot(
            String teacherId,
            AvailabilitySlot slot,
            Instant rangeStart,
            Instant rangeEnd
    ) {
        try {
            if (slot == null || slot.getStartTimeUtc() == null || slot.getEndTimeUtc() == null) {
                return null;
            }

            Instant slotStart = slot.getStartTimeUtc();
            Instant slotEnd = slot.getEndTimeUtc();

            if (!slotEnd.isAfter(slotStart)) {
                log.warn("Skipping invalid date-specific slot: {} - {}", slotStart, slotEnd);
                return null;
            }

            if (slotEnd.isBefore(rangeStart) || slotStart.isAfter(rangeEnd)) {
                return null;
            }

            boolean isBooked = isSlotBooked(teacherId, slotStart, slotEnd);

            return AvailabilityDto.builder()
                    .startTime(slotStart)
                    .endTime(slotEnd)
                    .isAvailable(!isBooked && Boolean.TRUE.equals(slot.getIsAvailable()))
                    .reason(isBooked ? "Session scheduled" : null)
                    .mode(slot.getMode())
                    .build();
        } catch (Exception ex) {
            log.error("Failed to process date-specific slot {} - {}: {}",
                    slot.getStartTimeUtc(),
                    slot.getEndTimeUtc(),
                    ex.getMessage(),
                    ex);
            return null;
        }
    }

    private boolean isSlotBooked(String teacherId, Instant slotStart, Instant slotEnd) {
        var bookings = bookingRepository.findByTeacherIdAndSessionStartTimeLessThanAndSessionEndTimeGreaterThan(
                teacherId,
                slotEnd,
                slotStart
        );

        boolean hasBlockingBooking = bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED
                        || b.getStatus() == BookingStatus.PENDING
                        || b.getStatus() == BookingStatus.PENDING_PAYMENT)
                .anyMatch(b ->
                        (b.getSessionStartTime() != null
                                && b.getSessionEndTime() != null
                                && b.getSessionStartTime().isBefore(slotEnd)
                                && b.getSessionEndTime().isAfter(slotStart))
                                ||
                                (b.getSessions() != null && b.getSessions().stream().anyMatch(session ->
                                        session.getStartTime() != null
                                                && session.getEndTime() != null
                                                && session.getStartTime().isBefore(slotEnd)
                                                && session.getEndTime().isAfter(slotStart)
                                ))
                );

        if (hasBlockingBooking) {
            return true;
        }

        Instant dayStart = slotStart.atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        Instant dayEnd = slotStart.atOffset(ZoneOffset.UTC)
                .toLocalDate()
                .plusDays(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        var sessions = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(teacherId, dayStart, dayEnd);

        return sessions.stream().anyMatch(session ->
                session.getScheduledStartTime() != null
                        && session.getScheduledEndTime() != null
                        && session.getScheduledStartTime().isBefore(slotEnd)
                        && session.getScheduledEndTime().isAfter(slotStart)
        );
    }

    private List<DateSpecificAvailability> loadDateSpecificEntries(String teacherId) {
        try {
            return dateSpecificAvailabilityRepository.findByTeacherId(teacherId);
        } catch (Exception ex) {
            log.warn("Failed to load date-specific availability for teacher {}: {}", teacherId, ex.getMessage());
            return new ArrayList<>();
        }
    }

    private LocalTime parseTime(String value) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            return LocalTime.parse(value);
        }
    }

    @SuppressWarnings("unused")
    private List<AvailabilityDto> getBookedSessionsOnly(String teacherId, Instant start, Instant end) {
        List<AvailabilityDto> bookedSlots = new ArrayList<>();

        var sessions = sessionRepository.findByTeacherIdAndScheduledStartTimeBetween(teacherId, start, end);

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