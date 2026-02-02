package com.tcon.learning_management_service.availability.controller;

import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.service.AvailabilityManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityManagementService availabilityManagementService;

    /**
     * Set teacher's weekly availability
     */
    @PostMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> setTeacherAvailability(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody Map<String, Object> request) {

        log.info("Setting availability for teacher: {}", teacherId);

        if (!userId.equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        @SuppressWarnings("unchecked")
        Map<DayOfWeek, List<TimeSlot>> weeklyAvailability =
                (Map<DayOfWeek, List<TimeSlot>>) request.getOrDefault("weeklyAvailability", new HashMap<>());

        String timezone = (String) request.getOrDefault("timezone", "UTC");
        Integer bufferTimeMinutes = (Integer) request.getOrDefault("bufferTimeMinutes", 15);
        Integer maxSessionsPerDay = (Integer) request.get("maxSessionsPerDay");

        TeacherAvailabilityDto availability = availabilityManagementService.setTeacherAvailability(
                teacherId,
                weeklyAvailability,
                timezone,
                bufferTimeMinutes,
                maxSessionsPerDay
        );

        return ResponseEntity.ok(availability);
    }

    /**
     * Get teacher's configured availability
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> getTeacherAvailability(
            @PathVariable String teacherId) {

        try {
            TeacherAvailabilityDto availability =
                    availabilityManagementService.getTeacherAvailability(teacherId);
            return ResponseEntity.ok(availability);
        } catch (IllegalArgumentException e) {
            log.warn("Teacher availability not found: {}", teacherId);
            // Return empty config instead of error
            return ResponseEntity.ok(TeacherAvailabilityDto.builder()
                    .teacherId(teacherId)
                    .timezone("UTC")
                    .bufferTimeMinutes(15)
                    .weeklyAvailability(new HashMap<>())
                    .build());
        }
    }

    /**
     * Delete teacher's availability
     */
    @DeleteMapping("/teacher/{teacherId}")
    public ResponseEntity<Void> deleteTeacherAvailability(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId) {

        if (!userId.equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        availabilityManagementService.deleteTeacherAvailability(teacherId);
        return ResponseEntity.noContent().build();
    }
}