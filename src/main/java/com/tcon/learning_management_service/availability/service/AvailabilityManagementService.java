package com.tcon.learning_management_service.availability.service;

import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityManagementService {

    private final TeacherAvailabilityRepository availabilityRepository;

    @Transactional
    public TeacherAvailabilityDto setTeacherAvailability(
            String teacherId,
            Map<DayOfWeek, List<TimeSlot>> weeklyAvailability,
            String timezone,
            Integer bufferTimeMinutes,
            Integer maxSessionsPerDay) {

        log.info("Setting availability for teacher: {}", teacherId);
        log.info("Received weekly availability: {}", weeklyAvailability);

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElse(TeacherAvailability.builder()
                        .teacherId(teacherId)
                        .weeklyAvailability(new HashMap<>())
                        .dateOverrides(new ArrayList<>())
                        .build());

        // ⭐ Set isAvailable to true for all slots if not set
        if (weeklyAvailability != null) {
            weeklyAvailability.forEach((day, slots) -> {
                if (slots != null) {
                    slots.forEach(slot -> {
                        if (slot.getIsAvailable() == null) {
                            slot.setIsAvailable(true);
                        }
                        log.info("Slot for {}: {} - {} (available: {})",
                                day, slot.getStartTime(), slot.getEndTime(), slot.getIsAvailable());
                    });
                }
            });
        }

        availability.setWeeklyAvailability(weeklyAvailability);
        availability.setTimezone(timezone != null ? timezone : "UTC");
        availability.setBufferTimeMinutes(bufferTimeMinutes != null ? bufferTimeMinutes : 15);
        availability.setMaxSessionsPerDay(maxSessionsPerDay);

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

        // Validate no overlap
        for (TimeSlot existing : daySlots) {
            if (timeSlotsOverlap(existing, timeSlot)) {
                log.error("Time slot overlaps: new ({} - {}) with existing ({} - {})",
                        timeSlot.getStartTime(), timeSlot.getEndTime(),
                        existing.getStartTime(), existing.getEndTime());
                throw new IllegalArgumentException("Time slot overlaps with existing slot");
            }
        }

        // ⭐ Set isAvailable if not set
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

    // ⭐ FIXED: Parse strings to LocalTime for comparison
    private boolean timeSlotsOverlap(TimeSlot slot1, TimeSlot slot2) {
        try {
            LocalTime start1 = LocalTime.parse(slot1.getStartTime());
            LocalTime end1 = LocalTime.parse(slot1.getEndTime());
            LocalTime start2 = LocalTime.parse(slot2.getStartTime());
            LocalTime end2 = LocalTime.parse(slot2.getEndTime());

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
                .build();
    }
}
