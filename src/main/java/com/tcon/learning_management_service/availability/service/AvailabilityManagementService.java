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

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElse(TeacherAvailability.builder()
                        .teacherId(teacherId)
                        .weeklyAvailability(new HashMap<>())
                        .dateOverrides(new ArrayList<>())
                        .build());

        availability.setWeeklyAvailability(weeklyAvailability);
        availability.setTimezone(timezone != null ? timezone : "UTC");
        availability.setBufferTimeMinutes(bufferTimeMinutes != null ? bufferTimeMinutes : 15);
        availability.setMaxSessionsPerDay(maxSessionsPerDay);

        TeacherAvailability saved = availabilityRepository.save(availability);
        log.info("Availability set successfully for teacher: {}", teacherId);

        return toDto(saved);
    }

    @Transactional
    public TeacherAvailabilityDto addTimeSlot(String teacherId, DayOfWeek dayOfWeek, TimeSlot timeSlot) {
        log.info("Adding time slot for teacher {} on {}", teacherId, dayOfWeek);

        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found"));

        List<TimeSlot> daySlots = availability.getWeeklyAvailability()
                .computeIfAbsent(dayOfWeek, k -> new ArrayList<>());

        // Validate no overlap
        for (TimeSlot existing : daySlots) {
            if (timeSlotsOverlap(existing, timeSlot)) {
                throw new IllegalArgumentException("Time slot overlaps with existing slot");
            }
        }

        daySlots.add(timeSlot);
        TeacherAvailability saved = availabilityRepository.save(availability);

        log.info("Time slot added successfully");
        return toDto(saved);
    }

    @Transactional
    public TeacherAvailabilityDto removeTimeSlot(String teacherId, DayOfWeek dayOfWeek,
                                                 TimeSlot timeSlot) {
        log.info("Removing time slot for teacher {} on {}", teacherId, dayOfWeek);

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
        log.info("Time slot removed successfully");

        return toDto(saved);
    }

    public TeacherAvailabilityDto getTeacherAvailability(String teacherId) {
        TeacherAvailability availability = availabilityRepository.findByTeacherId(teacherId)
                .orElseThrow(() -> new IllegalArgumentException("Teacher availability not found: " + teacherId));
        return toDto(availability);
    }

    @Transactional
    public void deleteTeacherAvailability(String teacherId) {
        log.info("Deleting availability for teacher: {}", teacherId);
        availabilityRepository.deleteByTeacherId(teacherId);
        log.info("Availability deleted successfully");
    }

    private boolean timeSlotsOverlap(TimeSlot slot1, TimeSlot slot2) {
        return !slot1.getEndTime().isBefore(slot2.getStartTime()) &&
                !slot1.getStartTime().isAfter(slot2.getEndTime());
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
