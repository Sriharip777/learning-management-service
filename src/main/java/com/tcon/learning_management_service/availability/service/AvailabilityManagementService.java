package com.tcon.learning_management_service.availability.service;

import com.tcon.learning_management_service.availability.dto.BatchDateAvailabilityRequest;
import com.tcon.learning_management_service.availability.dto.DateSpecificAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.SessionMode;
import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDto;
import com.tcon.learning_management_service.availability.entity.DateSpecificAvailability;
import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.repository.DateSpecificAvailabilityRepository;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
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

    private static final String DEFAULT_TIMEZONE = "UTC";
    private static final int DEFAULT_BUFFER_MINUTES = 15;
    private static final DateTimeFormatter FLEXIBLE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm[:ss]");

    private final TeacherAvailabilityRepository availabilityRepository;
    private final DateSpecificAvailabilityRepository dateSpecificRepository;
    private final TimezoneValidationService timezoneValidationService;

    @Transactional
    public TeacherAvailabilityDto setTeacherAvailability(
            String teacherId,
            Map<DayOfWeek, List<TimeSlot>> weeklyAvailability,
            String timezone,
            Integer bufferTimeMinutes,
            Integer maxSessionsPerDay,
            Boolean oneOnOneEnabled,
            Boolean groupEnabled,
            WeeklyPatternDto weeklyPattern) {

        validateTeacherId(teacherId);

        log.info("Setting weekly availability for teacher: {}", teacherId);

        String validatedTimezone = timezoneValidationService.validateAndNormalizeTimezone(
                timezone != null ? timezone : DEFAULT_TIMEZONE
        );

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseGet(() -> TeacherAvailability.builder()
                        .teacherId(teacherId)
                        .weeklyAvailability(new HashMap<>())
                        .dateOverrides(new ArrayList<>())
                        .build());

        Map<DayOfWeek, List<TimeSlot>> mergedWeeklyAvailability =
                Optional.ofNullable(availability.getWeeklyAvailability())
                        .map(HashMap::new)
                        .orElseGet(HashMap::new);

        if (weeklyAvailability != null && !weeklyAvailability.isEmpty()) {
            weeklyAvailability.forEach((day, incomingSlots) -> {
                if (day == null) {
                    throw new IllegalArgumentException("DayOfWeek cannot be null");
                }

                List<TimeSlot> normalizedIncomingSlots = normalizeSlots(incomingSlots, "weekly availability for " + day);
                List<TimeSlot> existingSlots = new ArrayList<>(
                        mergedWeeklyAvailability.getOrDefault(day, new ArrayList<>())
                );

                existingSlots.forEach(slot -> validateAndNormalizeSlot(slot, "existing weekly slot for " + day));

                List<TimeSlot> combined = new ArrayList<>(existingSlots);
                combined.addAll(normalizedIncomingSlots);

                validateNoOverlaps(combined, "weekly availability for " + day);
                mergedWeeklyAvailability.put(day, combined);
            });
        }

        availability.setWeeklyAvailability(mergedWeeklyAvailability);
        availability.setTimezone(validatedTimezone);
        availability.setBufferTimeMinutes(bufferTimeMinutes != null ? bufferTimeMinutes : DEFAULT_BUFFER_MINUTES);
        availability.setMaxSessionsPerDay(maxSessionsPerDay);
        availability.setOneOnOneEnabled(Boolean.TRUE.equals(oneOnOneEnabled));
        availability.setGroupEnabled(Boolean.TRUE.equals(groupEnabled));

        applyWeeklyPattern(availability, weeklyPattern);

        TeacherAvailability saved = availabilityRepository.save(availability);
        log.info("Weekly availability saved successfully for teacher: {}", teacherId);

        return toDto(saved);
    }

    @Transactional
    public void saveDateSpecificAvailabilityBatch(BatchDateAvailabilityRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("BatchDateAvailabilityRequest cannot be null");
        }

        validateTeacherId(request.getTeacherId());

        log.info("Saving batch date-specific availability for teacher: {}", request.getTeacherId());

        String validatedTimezone = timezoneValidationService.validateAndNormalizeTimezone(
                request.getTimezone() != null ? request.getTimezone() : DEFAULT_TIMEZONE
        );

        TeacherAvailability availability = availabilityRepository.findByTeacherId(request.getTeacherId())
                .orElseGet(() -> TeacherAvailability.builder()
                        .teacherId(request.getTeacherId())
                        .weeklyAvailability(new HashMap<>())
                        .dateOverrides(new ArrayList<>())
                        .build());

        availability.setTimezone(validatedTimezone);
        availability.setOneOnOneEnabled(Boolean.TRUE.equals(request.getOneOnOneEnabled()));
        availability.setGroupEnabled(Boolean.TRUE.equals(request.getGroupEnabled()));

        applyWeeklyPattern(availability, request.getWeeklyPattern());
        availabilityRepository.save(availability);

        if (request.getDateSlots() == null || request.getDateSlots().isEmpty()) {
            log.warn("No date-specific slots provided for teacher: {}", request.getTeacherId());
            return;
        }

        for (DateSpecificAvailabilityDto dateDto : request.getDateSlots()) {
            if (dateDto == null) {
                continue;
            }

            LocalDate date = parseDate(dateDto.getDate());
            List<TimeSlot> normalizedSlots = normalizeSlots(dateDto.getTimeSlots(), "date-specific slots for " + date);

            validateNoOverlaps(normalizedSlots, "date-specific availability for " + date);

            DateSpecificAvailability entity = DateSpecificAvailability.builder()
                    .teacherId(request.getTeacherId())
                    .date(date)
                    .timeSlots(normalizedSlots)
                    .timezone(validatedTimezone)
                    .bufferTimeMinutes(request.getBufferTimeMinutes() != null
                            ? request.getBufferTimeMinutes()
                            : DEFAULT_BUFFER_MINUTES)
                    .build();

            dateSpecificRepository.findByTeacherIdAndDate(request.getTeacherId(), date)
                    .ifPresent(dateSpecificRepository::delete);

            dateSpecificRepository.save(entity);
            log.info("Saved date-specific availability for teacher {} on {}", request.getTeacherId(), date);
        }
    }

    public TeacherAvailabilityDto getTeacherAvailability(String teacherId) {
        validateTeacherId(teacherId);

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found: " + teacherId));

        return toDto(availability);
    }

    @Transactional
    public TeacherAvailabilityDto addTimeSlot(String teacherId, DayOfWeek dayOfWeek, TimeSlot timeSlot) {
        validateTeacherId(teacherId);

        if (dayOfWeek == null) {
            throw new IllegalArgumentException("DayOfWeek is required");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("TimeSlot is required");
        }

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        Map<DayOfWeek, List<TimeSlot>> weeklyAvailability =
                Optional.ofNullable(availability.getWeeklyAvailability())
                        .orElseGet(HashMap::new);

        List<TimeSlot> daySlots = new ArrayList<>(
                weeklyAvailability.getOrDefault(dayOfWeek, new ArrayList<>())
        );

        TimeSlot normalizedSlot = validateAndNormalizeSlot(timeSlot, "new time slot for " + dayOfWeek);
        daySlots.add(normalizedSlot);

        validateNoOverlaps(daySlots, "weekly availability for " + dayOfWeek);

        weeklyAvailability.put(dayOfWeek, daySlots);
        availability.setWeeklyAvailability(weeklyAvailability);

        TeacherAvailability saved = availabilityRepository.save(availability);
        log.info("Added time slot for teacher {} on {}", teacherId, dayOfWeek);

        return toDto(saved);
    }

    @Transactional
    public TeacherAvailabilityDto removeTimeSlot(String teacherId, DayOfWeek dayOfWeek, TimeSlot timeSlot) {
        validateTeacherId(teacherId);

        if (dayOfWeek == null) {
            throw new IllegalArgumentException("DayOfWeek is required");
        }
        if (timeSlot == null) {
            throw new IllegalArgumentException("TimeSlot is required");
        }

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        Map<DayOfWeek, List<TimeSlot>> weeklyAvailability =
                Optional.ofNullable(availability.getWeeklyAvailability())
                        .orElseGet(HashMap::new);

        List<TimeSlot> daySlots = weeklyAvailability.get(dayOfWeek);

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
                weeklyAvailability.put(dayOfWeek, daySlots);
            }
        }

        availability.setWeeklyAvailability(weeklyAvailability);

        TeacherAvailability saved = availabilityRepository.save(availability);
        log.info("Removed time slot for teacher {} on {}", teacherId, dayOfWeek);

        return toDto(saved);
    }

    @Transactional
    public void deleteTeacherAvailability(String teacherId) {
        validateTeacherId(teacherId);
        availabilityRepository.deleteByTeacherId(teacherId);
        log.info("Deleted weekly availability for teacher: {}", teacherId);
    }

    public Map<String, List<TimeSlot>> getDateSpecificAvailability(String teacherId, SessionMode mode) {
        validateTeacherId(teacherId);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(6);

        List<DateSpecificAvailability> availabilities = dateSpecificRepository
                .findByTeacherIdAndDateBetween(teacherId, today, futureDate);

        Map<String, List<TimeSlot>> result = new LinkedHashMap<>();

        availabilities.stream()
                .sorted(Comparator.comparing(DateSpecificAvailability::getDate))
                .forEach(avail -> {
                    List<TimeSlot> allSlotsForDate = Optional.ofNullable(avail.getTimeSlots())
                            .orElseGet(List::of);

                    List<TimeSlot> filteredSlots = (mode == null)
                            ? allSlotsForDate
                            : allSlotsForDate.stream()
                            .filter(slot ->
                                    (mode == SessionMode.ONE_ON_ONE && slot.getMode() == null) ||
                                            slot.getMode() == mode)
                            .toList();

                    if (!filteredSlots.isEmpty()) {
                        result.put(avail.getDate().toString(), filteredSlots);
                    }
                });

        return result;
    }

    @Transactional
    public void deleteDateSpecificAvailability(String teacherId, LocalDate date) {
        validateTeacherId(teacherId);

        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        dateSpecificRepository.deleteByTeacherIdAndDate(teacherId, date);
        log.info("Deleted date-specific availability for teacher {} on {}", teacherId, date);
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

        if (weeklyPattern.getTimeStart() != null && !weeklyPattern.getTimeStart().isBlank()) {
            availability.setWeeklyPatternStart(normalizeTimeString(weeklyPattern.getTimeStart()));
        } else {
            availability.setWeeklyPatternStart(null);
        }

        if (weeklyPattern.getTimeEnd() != null && !weeklyPattern.getTimeEnd().isBlank()) {
            availability.setWeeklyPatternEnd(normalizeTimeString(weeklyPattern.getTimeEnd()));
        } else {
            availability.setWeeklyPatternEnd(null);
        }

        if (availability.getWeeklyPatternEnabled()) {
            if (availability.getWeeklyPatternDays().isEmpty()) {
                throw new IllegalArgumentException("Weekly pattern days are required when weekly pattern is enabled");
            }
            if (availability.getWeeklyPatternStart() == null || availability.getWeeklyPatternEnd() == null) {
                throw new IllegalArgumentException("Weekly pattern start and end time are required when weekly pattern is enabled");
            }

            LocalTime start = LocalTime.parse(availability.getWeeklyPatternStart(), FLEXIBLE_TIME_FORMATTER);
            LocalTime end = LocalTime.parse(availability.getWeeklyPatternEnd(), FLEXIBLE_TIME_FORMATTER);

            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Weekly pattern end time must be after start time");
            }
        }
    }

    private List<TimeSlot> normalizeSlots(List<TimeSlot> slots, String context) {
        if (slots == null || slots.isEmpty()) {
            return new ArrayList<>();
        }

        List<TimeSlot> normalized = new ArrayList<>();
        for (TimeSlot slot : slots) {
            normalized.add(validateAndNormalizeSlot(slot, context));
        }
        return normalized;
    }

    private TimeSlot validateAndNormalizeSlot(TimeSlot slot, String context) {
        if (slot == null) {
            throw new IllegalArgumentException("Time slot cannot be null for " + context);
        }
        if (slot.getStartTime() == null || slot.getStartTime().isBlank()) {
            throw new IllegalArgumentException("Start time is required for " + context);
        }
        if (slot.getEndTime() == null || slot.getEndTime().isBlank()) {
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
            throw new IllegalArgumentException("Invalid time format for " + context + ": " + e.getMessage());
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time for " + context);
        }

        return TimeSlot.builder()
                .startTime(normalizedStart)
                .endTime(normalizedEnd)
                .isAvailable(slot.getIsAvailable() != null ? slot.getIsAvailable() : true)
                .mode(slot.getMode())
                .build();
    }

    private void validateNoOverlaps(List<TimeSlot> slots, String context) {
        if (slots == null || slots.size() <= 1) {
            return;
        }

        List<TimeSlot> sortedSlots = slots.stream()
                .map(slot -> validateAndNormalizeSlot(slot, context))
                .sorted(Comparator.comparing(slot -> LocalTime.parse(slot.getStartTime(), FLEXIBLE_TIME_FORMATTER)))
                .toList();

        for (int i = 0; i < sortedSlots.size() - 1; i++) {
            TimeSlot current = sortedSlots.get(i);
            TimeSlot next = sortedSlots.get(i + 1);

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

    private String normalizeTimeString(String time) {
        try {
            LocalTime parsed = LocalTime.parse(time, FLEXIBLE_TIME_FORMATTER);
            return parsed.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + time);
        }
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format: " + date);
        }
    }

    private void validateTeacherId(String teacherId) {
        if (teacherId == null || teacherId.isBlank()) {
            throw new IllegalArgumentException("Teacher ID is required");
        }
    }

    private TeacherAvailabilityDto toDto(TeacherAvailability availability) {
        return TeacherAvailabilityDto.builder()
                .id(availability.getId())
                .teacherId(availability.getTeacherId())
                .timezone(availability.getTimezone())
                .weeklyAvailability(availability.getWeeklyAvailability())
                .bufferTimeMinutes(availability.getBufferTimeMinutes())
                .maxSessionsPerDay(availability.getMaxSessionsPerDay())
                .oneOnOneEnabled(availability.getOneOnOneEnabled())
                .groupEnabled(availability.getGroupEnabled())
                .weeklyPattern(
                        WeeklyPatternDto.builder()
                                .enabled(availability.getWeeklyPatternEnabled())
                                .days(availability.getWeeklyPatternDays())
                                .timeStart(availability.getWeeklyPatternStart())
                                .timeEnd(availability.getWeeklyPatternEnd())
                                .build()
                )
                .build();
    }
}