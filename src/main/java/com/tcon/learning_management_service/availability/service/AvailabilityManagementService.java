package com.tcon.learning_management_service.availability.service;

import com.tcon.learning_management_service.availability.dto.BatchDateAvailabilityRequest;
import com.tcon.learning_management_service.availability.dto.DateSpecificAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.entity.DateSpecificAvailability;
import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.repository.DateSpecificAvailabilityRepository;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDto;
import com.tcon.learning_management_service.availability.dto.SessionMode;
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

    // ==================== WEEKLY AVAILABILITY ====================

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
        log.info("Received weekly availability: {}", weeklyAvailability);

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
                daySlots.addAll(incomingSlots);
                existing.put(day, daySlots);
            });

            availability.setWeeklyAvailability(existing);
        }

        availability.setTimezone(timezone != null ? timezone : "UTC");
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
        log.info("Availability set successfully for teacher: {} with {} days configured",
                teacherId, saved.getWeeklyAvailability().size());

        return toDto(saved);
    }

    @Transactional
    public TeacherAvailabilityDto addTimeSlot(String teacherId, DayOfWeek dayOfWeek, TimeSlot timeSlot) {
        log.info("Adding time slot for teacher {} on {}: {} - {}",
                teacherId, dayOfWeek, timeSlot.getStartTime(), timeSlot.getEndTime());

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        List<TimeSlot> daySlots = availability.getWeeklyAvailability()
                .computeIfAbsent(dayOfWeek, k -> new ArrayList<>());

        for (TimeSlot existing : daySlots) {
            if (timeSlotsOverlap(existing, timeSlot)) {
                log.error("Time slot overlaps: new ({} - {}) with existing ({} - {})",
                        timeSlot.getStartTime(), timeSlot.getEndTime(),
                        existing.getStartTime(), existing.getEndTime());
                throw new IllegalArgumentException("Time slot overlaps with existing slot");
            }
        }

        if (timeSlot.getIsAvailable() == null) {
            timeSlot.setIsAvailable(true);
        }

        daySlots.add(timeSlot);
        TeacherAvailability saved = availabilityRepository.save(availability);

        log.info("Time slot added successfully. Total slots for {}: {}", dayOfWeek, daySlots.size());
        return toDto(saved);
    }

    @Transactional
    public TeacherAvailabilityDto removeTimeSlot(String teacherId, DayOfWeek dayOfWeek,
                                                 TimeSlot timeSlot) {
        log.info("Removing time slot for teacher {} on {}: {} - {}",
                teacherId, dayOfWeek, timeSlot.getStartTime(), timeSlot.getEndTime());

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        List<TimeSlot> daySlots = availability.getWeeklyAvailability().get(dayOfWeek);
        if (daySlots != null) {
            int initialSize = daySlots.size();
            daySlots.removeIf(slot ->
                    slot.getStartTime().equals(timeSlot.getStartTime()) &&
                            slot.getEndTime().equals(timeSlot.getEndTime())
            );
            log.info("Removed {} slot(s) from {}", initialSize - daySlots.size(), dayOfWeek);
        } else {
            log.warn("No slots found for {} to remove", dayOfWeek);
        }

        TeacherAvailability saved = availabilityRepository.save(availability);
        log.info("Time slot removed successfully");
        return toDto(saved);
    }

    public TeacherAvailabilityDto getTeacherAvailability(String teacherId) {
        log.info("Getting availability for teacher: {}", teacherId);

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found: " + teacherId));

        log.info("Found availability with {} days configured",
                availability.getWeeklyAvailability().size());

        return toDto(availability);
    }

    @Transactional
    public void deleteTeacherAvailability(String teacherId) {
        log.info("Deleting availability for teacher: {}", teacherId);
        availabilityRepository.deleteByTeacherId(teacherId);
        log.info("Availability deleted successfully");
    }

    // ==================== DATE-SPECIFIC AVAILABILITY ====================

    @Transactional
    public void saveDateSpecificAvailabilityBatch(BatchDateAvailabilityRequest request) {
        log.info("💾 Saving batch date-specific availability for teacher: {}", request.getTeacherId());

        TeacherAvailability availability = availabilityRepository
                .findByTeacherId(request.getTeacherId())
                .orElse(TeacherAvailability.builder()
                        .teacherId(request.getTeacherId())
                        .weeklyAvailability(new HashMap<>())
                        .dateOverrides(new ArrayList<>())
                        .build());

        availability.setOneOnOneEnabled(request.getOneOnOneEnabled());
        availability.setGroupEnabled(request.getGroupEnabled());

        if (request.getWeeklyPattern() != null) {
            WeeklyPatternDto p = request.getWeeklyPattern();
            availability.setWeeklyPatternEnabled(Boolean.TRUE.equals(p.getEnabled()));
            availability.setWeeklyPatternDays(
                    p.getDays() != null ? p.getDays() : new ArrayList<>());
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
                    .timezone(request.getTimezone())
                    .bufferTimeMinutes(request.getBufferTimeMinutes())
                    .build();

            dateSpecificRepository.findByTeacherIdAndDate(request.getTeacherId(), date)
                    .ifPresent(dateSpecificRepository::delete);

            dateSpecificRepository.save(entity);
            log.info("✅ Saved date-specific availability for {}", dateDto.getDate());
        }
    }

    public Map<String, List<TimeSlot>> getDateSpecificAvailability(String teacherId, SessionMode mode) {
        log.info("📅 Fetching date-specific availability for teacher: {} with mode: {}", teacherId, mode);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(6);

        List<DateSpecificAvailability> availabilities = dateSpecificRepository
                .findByTeacherIdAndDateBetween(teacherId, today, futureDate);

        Map<String, List<TimeSlot>> result = new HashMap<>();

        for (DateSpecificAvailability avail : availabilities) {
            String dateKey = avail.getDate().toString();
            List<TimeSlot> allSlotsForDate = avail.getTimeSlots();

            List<TimeSlot> filteredSlots;
            if (mode == null) {
                filteredSlots = allSlotsForDate;
            } else {
                filteredSlots = allSlotsForDate.stream()
                        .filter(slot ->
                                (mode == SessionMode.ONE_ON_ONE && slot.getMode() == null) ||
                                        slot.getMode() == mode
                        )
                        .toList();
            }

            if (!filteredSlots.isEmpty()) {
                result.put(dateKey, filteredSlots);
            }
        }

        log.info("✅ Found {} date-specific entries after mode filter", result.size());
        return result;
    }

    @Transactional
    public void deleteDateSpecificAvailability(String teacherId, LocalDate date) {
        log.info("🗑️ Deleting date-specific availability for teacher {} on {}", teacherId, date);
        dateSpecificRepository.deleteByTeacherIdAndDate(teacherId, date);
        log.info("✅ Deleted successfully");
    }

    // ==================== HELPER METHODS ====================

    private boolean timeSlotsOverlap(TimeSlot slot1, TimeSlot slot2) {
        try {
            LocalTime start1 = LocalTime.parse(slot1.getStartTime());
            LocalTime end1   = LocalTime.parse(slot1.getEndTime());
            LocalTime start2 = LocalTime.parse(slot2.getStartTime());
            LocalTime end2   = LocalTime.parse(slot2.getEndTime());

            boolean overlaps = !end1.isBefore(start2) && !start1.isAfter(end2);

            if (overlaps) {
                log.warn("Overlap detected: [{} - {}] and [{} - {}]", start1, end1, start2, end2);
            }

            return overlaps;

        } catch (DateTimeParseException e) {
            log.error("Failed to parse time slot: slot1({} - {}), slot2({} - {})",
                    slot1.getStartTime(), slot1.getEndTime(),
                    slot2.getStartTime(), slot2.getEndTime(), e);
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
