package com.tcon.learning_management_service.availability.service;

import com.tcon.learning_management_service.availability.dto.BatchDateAvailabilityRequest;
import com.tcon.learning_management_service.availability.dto.DateSpecificAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDto;
import com.tcon.learning_management_service.availability.dto.SessionMode;
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
import java.time.format.DateTimeParseException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityManagementService {

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

        log.info("Setting availability for teacher: {}", teacherId);

        String validatedTimezone = timezoneValidationService.validateAndNormalizeTimezone(timezone);

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElse(TeacherAvailability.builder()
                        .teacherId(teacherId)
                        .weeklyAvailability(new HashMap<>())
                        .dateOverrides(new ArrayList<>())
                        .build());

        if (weeklyAvailability != null) {
            Map<DayOfWeek, List<TimeSlot>> existing =
                    Optional.ofNullable(availability.getWeeklyAvailability())
                            .orElseGet(HashMap::new);

            weeklyAvailability.forEach((day, incomingSlots) -> {
                if (incomingSlots == null) return;
                List<TimeSlot> daySlots = existing.getOrDefault(day, new ArrayList<>());

                for (TimeSlot incomingSlot : incomingSlots) {
                    for (TimeSlot existingSlot : daySlots) {
                        if (timeSlotsOverlap(existingSlot, incomingSlot)) {
                            throw new IllegalArgumentException(
                                    "Time slot overlaps on " + day + ": " +
                                            incomingSlot.getStartTime() + " - " + incomingSlot.getEndTime()
                            );
                        }
                    }
                    if (incomingSlot.getIsAvailable() == null) {
                        incomingSlot.setIsAvailable(true);
                    }
                    daySlots.add(incomingSlot);
                }

                existing.put(day, daySlots);
            });

            availability.setWeeklyAvailability(existing);
        }

        availability.setTimezone(validatedTimezone);
        availability.setBufferTimeMinutes(bufferTimeMinutes != null ? bufferTimeMinutes : 15);
        availability.setMaxSessionsPerDay(maxSessionsPerDay);
        availability.setOneOnOneEnabled(oneOnOneEnabled);
        availability.setGroupEnabled(groupEnabled);

        if (weeklyPattern != null) {
            availability.setWeeklyPatternEnabled(Boolean.TRUE.equals(weeklyPattern.getEnabled()));
            availability.setWeeklyPatternDays(
                    weeklyPattern.getDays() != null ? weeklyPattern.getDays() : new ArrayList<>());
            availability.setWeeklyPatternStart(weeklyPattern.getTimeStart());
            availability.setWeeklyPatternEnd(weeklyPattern.getTimeEnd());
        }

        TeacherAvailability saved = availabilityRepository.save(availability);
        return toDto(saved);
    }

    @Transactional
    public void saveDateSpecificAvailabilityBatch(BatchDateAvailabilityRequest request) {
        log.info("Saving batch date-specific availability for teacher: {}", request.getTeacherId());

        String validatedTimezone = timezoneValidationService.validateAndNormalizeTimezone(request.getTimezone());

        TeacherAvailability availability = availabilityRepository
                .findByTeacherId(request.getTeacherId())
                .orElse(TeacherAvailability.builder()
                        .teacherId(request.getTeacherId())
                        .weeklyAvailability(new HashMap<>())
                        .dateOverrides(new ArrayList<>())
                        .build());

        availability.setTimezone(validatedTimezone);
        availability.setOneOnOneEnabled(request.getOneOnOneEnabled());
        availability.setGroupEnabled(request.getGroupEnabled());

        if (request.getWeeklyPattern() != null) {
            WeeklyPatternDto p = request.getWeeklyPattern();
            availability.setWeeklyPatternEnabled(Boolean.TRUE.equals(p.getEnabled()));
            availability.setWeeklyPatternDays(p.getDays() != null ? p.getDays() : new ArrayList<>());
            availability.setWeeklyPatternStart(p.getTimeStart());
            availability.setWeeklyPatternEnd(p.getTimeEnd());
        } else {
            availability.setWeeklyPatternEnabled(null);
            availability.setWeeklyPatternDays(new ArrayList<>());
            availability.setWeeklyPatternStart(null);
            availability.setWeeklyPatternEnd(null);
        }

        availabilityRepository.save(availability);

        for (DateSpecificAvailabilityDto dateDto : request.getDateSlots()) {
            LocalDate date = LocalDate.parse(dateDto.getDate());

            dateDto.getTimeSlots().forEach(slot -> {
                if (slot.getIsAvailable() == null) {
                    slot.setIsAvailable(true);
                }
            });

            DateSpecificAvailability entity = DateSpecificAvailability.builder()
                    .teacherId(request.getTeacherId())
                    .date(date)
                    .timeSlots(dateDto.getTimeSlots())
                    .timezone(validatedTimezone)
                    .bufferTimeMinutes(request.getBufferTimeMinutes())
                    .build();

            dateSpecificRepository.findByTeacherIdAndDate(request.getTeacherId(), date)
                    .ifPresent(dateSpecificRepository::delete);

            dateSpecificRepository.save(entity);
        }
    }

    public TeacherAvailabilityDto getTeacherAvailability(String teacherId) {
        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found: " + teacherId));

        return toDto(availability);
    }

    @Transactional
    public TeacherAvailabilityDto addTimeSlot(String teacherId, DayOfWeek dayOfWeek, TimeSlot timeSlot) {
        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        List<TimeSlot> daySlots = availability.getWeeklyAvailability()
                .computeIfAbsent(dayOfWeek, k -> new ArrayList<>());

        for (TimeSlot existing : daySlots) {
            if (timeSlotsOverlap(existing, timeSlot)) {
                throw new IllegalArgumentException("Time slot overlaps with existing slot");
            }
        }

        if (timeSlot.getIsAvailable() == null) {
            timeSlot.setIsAvailable(true);
        }

        daySlots.add(timeSlot);
        TeacherAvailability saved = availabilityRepository.save(availability);
        return toDto(saved);
    }

    @Transactional
    public TeacherAvailabilityDto removeTimeSlot(String teacherId, DayOfWeek dayOfWeek, TimeSlot timeSlot) {
        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        List<TimeSlot> daySlots = availability.getWeeklyAvailability().get(dayOfWeek);
        if (daySlots != null) {
            daySlots.removeIf(slot ->
                    slot.getStartTime().equals(timeSlot.getStartTime()) &&
                            slot.getEndTime().equals(timeSlot.getEndTime())
            );
        }

        TeacherAvailability saved = availabilityRepository.save(availability);
        return toDto(saved);
    }

    @Transactional
    public void deleteTeacherAvailability(String teacherId) {
        availabilityRepository.deleteByTeacherId(teacherId);
    }

    public Map<String, List<TimeSlot>> getDateSpecificAvailability(String teacherId, SessionMode mode) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(6);

        List<DateSpecificAvailability> availabilities = dateSpecificRepository
                .findByTeacherIdAndDateBetween(teacherId, today, futureDate);

        Map<String, List<TimeSlot>> result = new HashMap<>();

        for (DateSpecificAvailability avail : availabilities) {
            String dateKey = avail.getDate().toString();
            List<TimeSlot> allSlotsForDate = avail.getTimeSlots();

            List<TimeSlot> filteredSlots = (mode == null)
                    ? allSlotsForDate
                    : allSlotsForDate.stream()
                    .filter(slot ->
                            (mode == SessionMode.ONE_ON_ONE && slot.getMode() == null) ||
                                    slot.getMode() == mode)
                    .toList();

            if (!filteredSlots.isEmpty()) {
                result.put(dateKey, filteredSlots);
            }
        }

        return result;
    }

    @Transactional
    public void deleteDateSpecificAvailability(String teacherId, LocalDate date) {
        dateSpecificRepository.deleteByTeacherIdAndDate(teacherId, date);
    }

    private boolean timeSlotsOverlap(TimeSlot slot1, TimeSlot slot2) {
        try {
            LocalTime start1 = LocalTime.parse(slot1.getStartTime());
            LocalTime end1 = LocalTime.parse(slot1.getEndTime());
            LocalTime start2 = LocalTime.parse(slot2.getStartTime());
            LocalTime end2 = LocalTime.parse(slot2.getEndTime());

            return !end1.isBefore(start2) && !start1.isAfter(end2);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid time format: " + e.getMessage());
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