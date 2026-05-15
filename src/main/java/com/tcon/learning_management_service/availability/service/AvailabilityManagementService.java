package com.tcon.learning_management_service.availability.service;

import com.tcon.learning_management_service.availability.dto.AvailabilitySlotDto;
import com.tcon.learning_management_service.availability.dto.BatchDateAvailabilityRequest;
import com.tcon.learning_management_service.availability.dto.DateSpecificAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.SessionMode;
import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDto;
import com.tcon.learning_management_service.availability.entity.AvailabilitySlot;
import com.tcon.learning_management_service.availability.entity.DateSpecificAvailability;
import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.entity.WeeklyTimeSlot;
import com.tcon.learning_management_service.availability.repository.DateSpecificAvailabilityRepository;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import com.tcon.learning_management_service.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityManagementService {

    private static final int DEFAULT_BUFFER_MINUTES = 15;
    private static final DateTimeFormatter FLEXIBLE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm[:ss]");
    private static final DateTimeFormatter NORMALIZED_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    private final TeacherAvailabilityRepository availabilityRepository;
    private final DateSpecificAvailabilityRepository dateSpecificRepository;
    private final BookingRepository bookingRepository;

    @Transactional
    public TeacherAvailabilityDto setTeacherAvailability(
            String teacherId,
            Map<DayOfWeek, List<WeeklyTimeSlot>> weeklyAvailability,
            Integer bufferTimeMinutes,
            Integer maxSessionsPerDay,
            Boolean oneOnOneEnabled,
            Boolean groupEnabled,
            WeeklyPatternDto weeklyPattern) {

        validateTeacherId(teacherId);

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseGet(() -> TeacherAvailability.builder()
                        .teacherId(teacherId)
                        .weeklyAvailability(new HashMap<>())
                        .bufferTimeMinutes(DEFAULT_BUFFER_MINUTES)
                        .oneOnOneEnabled(Boolean.TRUE)
                        .groupEnabled(Boolean.FALSE)
                        .build());

        Map<DayOfWeek, List<WeeklyTimeSlot>> mergedWeeklyAvailability =
                Optional.ofNullable(availability.getWeeklyAvailability())
                        .map(HashMap::new)
                        .orElseGet(HashMap::new);

        if (weeklyAvailability != null && !weeklyAvailability.isEmpty()) {
            weeklyAvailability.forEach((day, incomingSlots) -> {
                if (day == null) {
                    throw new IllegalArgumentException("DayOfWeek cannot be null");
                }

                List<WeeklyTimeSlot> normalizedIncomingSlots =
                        normalizeWeeklySlots(incomingSlots, "weekly availability for " + day);

                List<WeeklyTimeSlot> finalSlots = deduplicateAndSortWeeklySlots(normalizedIncomingSlots);
                validateNoWeeklyOverlaps(finalSlots, "weekly availability for " + day);
                mergedWeeklyAvailability.put(day, finalSlots);
            });
        }

        availability.setWeeklyAvailability(mergedWeeklyAvailability);
        availability.setBufferTimeMinutes(bufferTimeMinutes != null ? bufferTimeMinutes : DEFAULT_BUFFER_MINUTES);
        availability.setMaxSessionsPerDay(maxSessionsPerDay);
        availability.setOneOnOneEnabled(Boolean.TRUE.equals(oneOnOneEnabled));
        availability.setGroupEnabled(Boolean.TRUE.equals(groupEnabled));

        applyWeeklyPattern(availability, weeklyPattern);

        TeacherAvailability saved = availabilityRepository.save(availability);
        return toTeacherAvailabilityDto(saved);
    }

    @Transactional(readOnly = true)
    public TeacherAvailabilityDto getTeacherAvailability(String teacherId) {
        validateTeacherId(teacherId);

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found: " + teacherId));

        return toTeacherAvailabilityDto(availability);
    }

    @Transactional
    public TeacherAvailabilityDto addTimeSlot(String teacherId, DayOfWeek dayOfWeek, WeeklyTimeSlot timeSlot) {
        validateTeacherId(teacherId);

        if (dayOfWeek == null) {
            throw new IllegalArgumentException("DayOfWeek is required");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("TimeSlot is required");
        }

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        Map<DayOfWeek, List<WeeklyTimeSlot>> weeklyAvailability =
                Optional.ofNullable(availability.getWeeklyAvailability())
                        .map(HashMap::new)
                        .orElseGet(HashMap::new);

        List<WeeklyTimeSlot> daySlots = new ArrayList<>(weeklyAvailability.getOrDefault(dayOfWeek, List.of()));
        daySlots.add(validateAndNormalizeWeeklySlot(timeSlot, "new time slot for " + dayOfWeek));

        List<WeeklyTimeSlot> finalSlots = deduplicateAndSortWeeklySlots(daySlots);
        validateNoWeeklyOverlaps(finalSlots, "weekly availability for " + dayOfWeek);

        weeklyAvailability.put(dayOfWeek, finalSlots);
        availability.setWeeklyAvailability(weeklyAvailability);

        return toTeacherAvailabilityDto(availabilityRepository.save(availability));
    }

    @Transactional
    public TeacherAvailabilityDto removeTimeSlot(String teacherId, DayOfWeek dayOfWeek, WeeklyTimeSlot timeSlot) {
        validateTeacherId(teacherId);

        if (dayOfWeek == null) {
            throw new IllegalArgumentException("DayOfWeek is required");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("TimeSlot is required");
        }

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        Map<DayOfWeek, List<WeeklyTimeSlot>> weeklyAvailability =
                Optional.ofNullable(availability.getWeeklyAvailability())
                        .map(HashMap::new)
                        .orElseGet(HashMap::new);

        List<WeeklyTimeSlot> daySlots = weeklyAvailability.get(dayOfWeek);

        if (daySlots != null && !daySlots.isEmpty()) {
            String normalizedStart = normalizeTimeString(timeSlot.getStartTime());
            String normalizedEnd = normalizeTimeString(timeSlot.getEndTime());

            daySlots.removeIf(slot ->
                    normalizeTimeString(slot.getStartTime()).equals(normalizedStart) &&
                            normalizeTimeString(slot.getEndTime()).equals(normalizedEnd)
            );

            if (daySlots.isEmpty()) {
                weeklyAvailability.remove(dayOfWeek);
            } else {
                weeklyAvailability.put(dayOfWeek, deduplicateAndSortWeeklySlots(daySlots));
            }
        }

        availability.setWeeklyAvailability(weeklyAvailability);
        return toTeacherAvailabilityDto(availabilityRepository.save(availability));
    }

    @Transactional
    public void deleteTeacherAvailability(String teacherId) {
        validateTeacherId(teacherId);
        availabilityRepository.deleteByTeacherId(teacherId);
    }

    @Transactional
    public void saveDateSpecificAvailabilityBatch(BatchDateAvailabilityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("BatchDateAvailabilityRequest cannot be null");
        }

        validateTeacherId(request.getTeacherId());

        TeacherAvailability availability = availabilityRepository.findByTeacherId(request.getTeacherId())
                .orElseGet(() -> TeacherAvailability.builder()
                        .teacherId(request.getTeacherId())
                        .weeklyAvailability(new HashMap<>())
                        .bufferTimeMinutes(DEFAULT_BUFFER_MINUTES)
                        .oneOnOneEnabled(Boolean.TRUE)
                        .groupEnabled(Boolean.FALSE)
                        .build());

        availability.setBufferTimeMinutes(
                request.getBufferTimeMinutes() != null ? request.getBufferTimeMinutes() : DEFAULT_BUFFER_MINUTES
        );
        availability.setOneOnOneEnabled(Boolean.TRUE.equals(request.getOneOnOneEnabled()));
        availability.setGroupEnabled(Boolean.TRUE.equals(request.getGroupEnabled()));
        applyWeeklyPattern(availability, request.getWeeklyPattern());
        availabilityRepository.save(availability);

        if (request.getDateSlots() == null || request.getDateSlots().isEmpty()) {
            return;
        }

        for (DateSpecificAvailabilityDto dateDto : request.getDateSlots()) {
            if (dateDto == null) {
                continue;
            }

            validateDateSpecificDto(dateDto);

            List<AvailabilitySlot> incomingSlots = normalizeAvailabilitySlots(
                    toEntitySlots(dateDto.getSlots()),
                    "date-specific slots for " + dateDto.getDayStartUtc()
            );

            List<AvailabilitySlot> finalSlots = deduplicateAndSortAvailabilitySlots(incomingSlots);
            validateNoUtcOverlaps(finalSlots, "date-specific availability for " + dateDto.getDayStartUtc());

            DateSpecificAvailability entity = dateSpecificRepository
                    .findByTeacherIdAndDayStartUtc(request.getTeacherId(), dateDto.getDayStartUtc())
                    .orElseGet(() -> DateSpecificAvailability.builder()
                            .teacherId(request.getTeacherId())
                            .dayStartUtc(dateDto.getDayStartUtc())
                            .build());

            entity.setTeacherId(request.getTeacherId());
            entity.setDayStartUtc(dateDto.getDayStartUtc());
            entity.setSlots(finalSlots);
            entity.setBufferTimeMinutes(
                    request.getBufferTimeMinutes() != null ? request.getBufferTimeMinutes() : DEFAULT_BUFFER_MINUTES
            );

            dateSpecificRepository.save(entity);
        }
    }

    @Transactional(readOnly = true)
    public List<DateSpecificAvailabilityDto> getDateSpecificAvailability(String teacherId, SessionMode mode) {
        validateTeacherId(teacherId);

        List<DateSpecificAvailability> availabilities = dateSpecificRepository.findByTeacherId(teacherId);

        List<Booking> teacherBookings = bookingRepository.findByTeacherId(teacherId).stream()
                .filter(this::blocksAvailability)
                .toList();

        return availabilities.stream()
                .sorted(Comparator.comparing(DateSpecificAvailability::getDayStartUtc))
                .map(avail -> {
                    List<AvailabilitySlot> allSlotsForDate = Optional.ofNullable(avail.getSlots()).orElseGet(List::of);

                    List<AvailabilitySlot> filteredSlots = mode == null
                            ? allSlotsForDate
                            : allSlotsForDate.stream()
                            .filter(slot -> matchesMode(slot, mode))
                            .toList();

                    List<AvailabilitySlotDto> reconciledSlots = filteredSlots.stream()
                            .map(slot -> reconcileSlotWithBookings(slot, teacherBookings))
                            .map(this::toSlotDto)
                            .toList();

                    return DateSpecificAvailabilityDto.builder()
                            .dayStartUtc(avail.getDayStartUtc())
                            .slots(reconciledSlots)
                            .build();
                })
                .toList();
    }

    @Transactional
    public void deleteDateSpecificAvailability(String teacherId, Instant dayStartUtc) {
        validateTeacherId(teacherId);

        if (dayStartUtc == null) {
            throw new IllegalArgumentException("dayStartUtc is required");
        }

        dateSpecificRepository.deleteByTeacherIdAndDayStartUtc(teacherId, dayStartUtc);
    }

    private void applyWeeklyPattern(TeacherAvailability availability, WeeklyPatternDto weeklyPattern) {
        if (weeklyPattern == null) {
            availability.setWeeklyPatternEnabled(false);
            availability.setWeeklyPatternDays(new ArrayList<>());
            availability.setWeeklyPatternStart(null);
            availability.setWeeklyPatternEnd(null);
            return;
        }

        availability.setWeeklyPatternEnabled(Boolean.TRUE.equals(weeklyPattern.getEnabled()));
        availability.setWeeklyPatternDays(
                weeklyPattern.getDays() != null ? new ArrayList<>(weeklyPattern.getDays()) : new ArrayList<>()
        );

        availability.setWeeklyPatternStart(hasText(weeklyPattern.getTimeStart())
                ? normalizeTimeString(weeklyPattern.getTimeStart())
                : null);

        availability.setWeeklyPatternEnd(hasText(weeklyPattern.getTimeEnd())
                ? normalizeTimeString(weeklyPattern.getTimeEnd())
                : null);

        if (Boolean.TRUE.equals(availability.getWeeklyPatternEnabled())) {
            if (availability.getWeeklyPatternDays() == null || availability.getWeeklyPatternDays().isEmpty()) {
                throw new IllegalArgumentException("Weekly pattern days are required when weekly pattern is enabled");
            }
            if (!hasText(availability.getWeeklyPatternStart()) || !hasText(availability.getWeeklyPatternEnd())) {
                throw new IllegalArgumentException("Weekly pattern start and end time are required when weekly pattern is enabled");
            }

            LocalTime start = LocalTime.parse(availability.getWeeklyPatternStart(), FLEXIBLE_TIME_FORMATTER);
            LocalTime end = LocalTime.parse(availability.getWeeklyPatternEnd(), FLEXIBLE_TIME_FORMATTER);

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Weekly pattern end time must be after start time");
            }
        }
    }

    private List<WeeklyTimeSlot> normalizeWeeklySlots(List<WeeklyTimeSlot> slots, String context) {
        if (slots == null || slots.isEmpty()) {
            return new ArrayList<>();
        }

        List<WeeklyTimeSlot> normalized = new ArrayList<>();
        for (WeeklyTimeSlot slot : slots) {
            normalized.add(validateAndNormalizeWeeklySlot(slot, context));
        }
        return normalized;
    }

    private WeeklyTimeSlot validateAndNormalizeWeeklySlot(WeeklyTimeSlot slot, String context) {
        if (slot == null) {
            throw new IllegalArgumentException("Time slot cannot be null for " + context);
        }
        if (!hasText(slot.getStartTime())) {
            throw new IllegalArgumentException("Start time is required for " + context);
        }
        if (!hasText(slot.getEndTime())) {
            throw new IllegalArgumentException("End time is required for " + context);
        }

        String normalizedStart = normalizeTimeString(slot.getStartTime());
        String normalizedEnd = normalizeTimeString(slot.getEndTime());

        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(normalizedStart, FLEXIBLE_TIME_FORMATTER);
            end = LocalTime.parse(normalizedEnd, FLEXIBLE_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format for " + context + ": " + e.getMessage(), e);
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time for " + context);
        }

        return WeeklyTimeSlot.builder()
                .startTime(normalizedStart)
                .endTime(normalizedEnd)
                .isAvailable(slot.getIsAvailable() != null ? slot.getIsAvailable() : true)
                .mode(slot.getMode())
                .build();
    }

    private List<WeeklyTimeSlot> deduplicateAndSortWeeklySlots(List<WeeklyTimeSlot> slots) {
        Map<String, WeeklyTimeSlot> uniqueSlots = new LinkedHashMap<>();

        for (WeeklyTimeSlot slot : slots) {
            if (slot == null) {
                continue;
            }

            WeeklyTimeSlot normalizedSlot = validateAndNormalizeWeeklySlot(slot, "slot merge");
            String key = normalizedSlot.getStartTime() + "_" +
                    normalizedSlot.getEndTime() + "_" +
                    (normalizedSlot.getMode() != null ? normalizedSlot.getMode().name() : "NULL");

            uniqueSlots.put(key, normalizedSlot);
        }

        List<WeeklyTimeSlot> finalSlots = new ArrayList<>(uniqueSlots.values());
        finalSlots.sort(Comparator.comparing(s -> LocalTime.parse(s.getStartTime(), FLEXIBLE_TIME_FORMATTER)));
        return finalSlots;
    }

    private void validateNoWeeklyOverlaps(List<WeeklyTimeSlot> slots, String context) {
        if (slots == null || slots.size() <= 1) {
            return;
        }

        List<WeeklyTimeSlot> sortedSlots = slots.stream()
                .map(slot -> validateAndNormalizeWeeklySlot(slot, context))
                .sorted(Comparator.comparing(slot -> LocalTime.parse(slot.getStartTime(), FLEXIBLE_TIME_FORMATTER)))
                .toList();

        for (int i = 0; i < sortedSlots.size() - 1; i++) {
            WeeklyTimeSlot current = sortedSlots.get(i);
            WeeklyTimeSlot next = sortedSlots.get(i + 1);

            LocalTime currentEnd = LocalTime.parse(current.getEndTime(), FLEXIBLE_TIME_FORMATTER);
            LocalTime nextStart = LocalTime.parse(next.getStartTime(), FLEXIBLE_TIME_FORMATTER);

            if (!currentEnd.isBefore(nextStart)) {
                throw new IllegalArgumentException(
                        "Time slot overlaps in " + context + ": " +
                                current.getStartTime() + "-" + current.getEndTime() +
                                " overlaps with " +
                                next.getStartTime() + "-" + next.getEndTime()
                );
            }
        }
    }

    private List<AvailabilitySlot> toEntitySlots(List<AvailabilitySlotDto> slots) {
        if (slots == null) {
            return new ArrayList<>();
        }

        return slots.stream()
                .map(slot -> AvailabilitySlot.builder()
                        .startTimeUtc(slot.getStartTimeUtc())
                        .endTimeUtc(slot.getEndTimeUtc())
                        .isAvailable(slot.getIsAvailable() != null ? slot.getIsAvailable() : true)
                        .mode(slot.getMode())
                        .build())
                .toList();
    }

    private List<AvailabilitySlot> normalizeAvailabilitySlots(List<AvailabilitySlot> slots, String context) {
        if (slots == null || slots.isEmpty()) {
            return new ArrayList<>();
        }

        List<AvailabilitySlot> normalized = new ArrayList<>();
        for (AvailabilitySlot slot : slots) {
            normalized.add(validateAndNormalizeAvailabilitySlot(slot, context));
        }
        return normalized;
    }

    private AvailabilitySlot validateAndNormalizeAvailabilitySlot(AvailabilitySlot slot, String context) {
        if (slot == null) {
            throw new IllegalArgumentException("Availability slot cannot be null for " + context);
        }
        if (slot.getStartTimeUtc() == null) {
            throw new IllegalArgumentException("startTimeUtc is required for " + context);
        }
        if (slot.getEndTimeUtc() == null) {
            throw new IllegalArgumentException("endTimeUtc is required for " + context);
        }
        if (!slot.getEndTimeUtc().isAfter(slot.getStartTimeUtc())) {
            throw new IllegalArgumentException("endTimeUtc must be after startTimeUtc for " + context);
        }

        return AvailabilitySlot.builder()
                .startTimeUtc(slot.getStartTimeUtc())
                .endTimeUtc(slot.getEndTimeUtc())
                .isAvailable(slot.getIsAvailable() != null ? slot.getIsAvailable() : true)
                .mode(slot.getMode())
                .build();
    }

    private List<AvailabilitySlot> deduplicateAndSortAvailabilitySlots(List<AvailabilitySlot> slots) {
        Map<String, AvailabilitySlot> uniqueSlots = new LinkedHashMap<>();

        for (AvailabilitySlot slot : slots) {
            if (slot == null) {
                continue;
            }

            AvailabilitySlot normalizedSlot = validateAndNormalizeAvailabilitySlot(slot, "slot merge");
            String key = normalizedSlot.getStartTimeUtc() + "_" +
                    normalizedSlot.getEndTimeUtc() + "_" +
                    (normalizedSlot.getMode() != null ? normalizedSlot.getMode().name() : "NULL");

            uniqueSlots.put(key, normalizedSlot);
        }

        List<AvailabilitySlot> finalSlots = new ArrayList<>(uniqueSlots.values());
        finalSlots.sort(Comparator.comparing(AvailabilitySlot::getStartTimeUtc));
        return finalSlots;
    }

    private void validateNoUtcOverlaps(List<AvailabilitySlot> slots, String context) {
        if (slots == null || slots.size() <= 1) {
            return;
        }

        List<AvailabilitySlot> sortedSlots = slots.stream()
                .map(slot -> validateAndNormalizeAvailabilitySlot(slot, context))
                .sorted(Comparator.comparing(AvailabilitySlot::getStartTimeUtc))
                .toList();

        for (int i = 0; i < sortedSlots.size() - 1; i++) {
            AvailabilitySlot current = sortedSlots.get(i);
            AvailabilitySlot next = sortedSlots.get(i + 1);

            if (!current.getEndTimeUtc().isBefore(next.getStartTimeUtc())) {
                throw new IllegalArgumentException(
                        "UTC slot overlaps in " + context + ": " +
                                current.getStartTimeUtc() + " - " + current.getEndTimeUtc() +
                                " overlaps with " +
                                next.getStartTimeUtc() + " - " + next.getEndTimeUtc()
                );
            }
        }
    }

    private boolean matchesMode(AvailabilitySlot slot, SessionMode mode) {
        if (mode == null) {
            return true;
        }
        if (slot == null) {
            return false;
        }
        if (mode == SessionMode.BOTH) {
            return true;
        }
        if (slot.getMode() == null) {
            return mode == SessionMode.ONE_ON_ONE;
        }
        return slot.getMode() == mode || slot.getMode() == SessionMode.BOTH;
    }

    private AvailabilitySlot reconcileSlotWithBookings(AvailabilitySlot slot, List<Booking> teacherBookings) {
        AvailabilitySlot normalizedSlot = validateAndNormalizeAvailabilitySlot(slot, "availability reconciliation");

        boolean occupied = teacherBookings.stream()
                .anyMatch(booking -> bookingOccupiesSlot(booking, normalizedSlot));

        return AvailabilitySlot.builder()
                .startTimeUtc(normalizedSlot.getStartTimeUtc())
                .endTimeUtc(normalizedSlot.getEndTimeUtc())
                .mode(normalizedSlot.getMode())
                .isAvailable(Boolean.TRUE.equals(normalizedSlot.getIsAvailable()) && !occupied)
                .build();
    }

    private boolean bookingOccupiesSlot(Booking booking, AvailabilitySlot slot) {
        if (booking == null || slot == null) {
            return false;
        }

        if (booking.getSessions() != null && !booking.getSessions().isEmpty()) {
            return booking.getSessions().stream()
                    .anyMatch(session -> overlapsWithSlot(slot, session.getStartTime(), session.getEndTime()));
        }

        return overlapsWithSlot(slot, booking.getSessionStartTime(), booking.getSessionEndTime());
    }

    private boolean overlapsWithSlot(AvailabilitySlot slot, Instant bookingStart, Instant bookingEnd) {
        if (bookingStart == null || bookingEnd == null) {
            return false;
        }

        return bookingStart.isBefore(slot.getEndTimeUtc())
                && bookingEnd.isAfter(slot.getStartTimeUtc());
    }

    private boolean blocksAvailability(Booking booking) {
        if (booking == null || booking.getStatus() == null) {
            return false;
        }

        return booking.getStatus() == BookingStatus.CONFIRMED
                || booking.getStatus() == BookingStatus.PENDING
                || booking.getStatus() == BookingStatus.PENDING_PAYMENT;
    }

    private String normalizeTimeString(String time) {
        try {
            LocalTime parsed = LocalTime.parse(time, FLEXIBLE_TIME_FORMATTER);
            return parsed.format(NORMALIZED_TIME_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + time, e);
        }
    }

    private void validateDateSpecificDto(DateSpecificAvailabilityDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DateSpecificAvailabilityDto cannot be null");
        }
        if (dto.getDayStartUtc() == null) {
            throw new IllegalArgumentException("dayStartUtc is required");
        }
        if (dto.getSlots() == null) {
            throw new IllegalArgumentException("slots are required");
        }
    }

    private void validateTeacherId(String teacherId) {
        if (!hasText(teacherId)) {
            throw new IllegalArgumentException("Teacher ID is required");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private AvailabilitySlotDto toSlotDto(AvailabilitySlot slot) {
        return AvailabilitySlotDto.builder()
                .startTimeUtc(slot.getStartTimeUtc())
                .endTimeUtc(slot.getEndTimeUtc())
                .isAvailable(slot.getIsAvailable())
                .mode(slot.getMode())
                .build();
    }

    private TeacherAvailabilityDto toTeacherAvailabilityDto(TeacherAvailability availability) {
        return TeacherAvailabilityDto.builder()
                .id(availability.getId())
                .teacherId(availability.getTeacherId())
                .weeklyAvailability(
                        availability.getWeeklyAvailability() != null
                                ? availability.getWeeklyAvailability()
                                : new HashMap<>()
                )
                .bufferTimeMinutes(availability.getBufferTimeMinutes())
                .maxSessionsPerDay(availability.getMaxSessionsPerDay())
                .oneOnOneEnabled(availability.getOneOnOneEnabled())
                .groupEnabled(availability.getGroupEnabled())
                .weeklyPattern(
                        WeeklyPatternDto.builder()
                                .enabled(Boolean.TRUE.equals(availability.getWeeklyPatternEnabled()))
                                .days(
                                        availability.getWeeklyPatternDays() != null
                                                ? availability.getWeeklyPatternDays()
                                                : new ArrayList<>()
                                )
                                .timeStart(availability.getWeeklyPatternStart())
                                .timeEnd(availability.getWeeklyPatternEnd())
                                .build()
                )
                .build();
    }
}