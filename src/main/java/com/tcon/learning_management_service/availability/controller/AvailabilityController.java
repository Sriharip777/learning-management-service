package com.tcon.learning_management_service.availability.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.service.AvailabilityManagementService;
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
    private final ObjectMapper objectMapper;

    /**
     * Set teacher's weekly availability
     * POST /api/availability/teacher/{teacherId}
     */
    @PostMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> setTeacherAvailability(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody AvailabilityRequest request) {

        log.info("POST /api/availability/teacher/{} - Setting availability", teacherId);
        log.info("User ID: {}", userId);
        log.info("Request body: {}", request);

        // Authorization check
        if (!userId.equals(teacherId)) {
            log.error("Unauthorized: user {} trying to set availability for teacher {}", userId, teacherId);
            throw new IllegalArgumentException("Unauthorized: You can only set your own availability");
        }

        try {
            // Convert weeklyAvailability from request
            Map<DayOfWeek, List<TimeSlot>> weeklyAvailability = convertWeeklyAvailability(request.getWeeklyAvailability());

            log.info("Converted weekly availability: {}", weeklyAvailability);

            TeacherAvailabilityDto availability = availabilityManagementService.setTeacherAvailability(
                    teacherId,
                    weeklyAvailability,
                    request.getTimezone(),
                    request.getBufferTimeMinutes(),
                    request.getMaxSessionsPerDay()
            );

            log.info("Successfully set availability for teacher: {}", teacherId);
            return ResponseEntity.ok(availability);

        } catch (Exception e) {
            log.error("Error setting availability for teacher {}: {}", teacherId, e.getMessage(), e);
            throw new RuntimeException("Failed to set availability: " + e.getMessage(), e);
        }
    }

    /**
     * Get teacher's configured availability
     * GET /api/availability/teacher/{teacherId}
     */
    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> getTeacherAvailability(
            @PathVariable String teacherId) {

        log.info("GET /api/availability/teacher/{} - Getting availability", teacherId);

        try {
            TeacherAvailabilityDto availability =
                    availabilityManagementService.getTeacherAvailability(teacherId);

            log.info("Found availability for teacher {} with {} days configured",
                    teacherId, availability.getWeeklyAvailability().size());

            return ResponseEntity.ok(availability);

        } catch (IllegalArgumentException e) {
            log.warn("Teacher availability not found: {} - Returning empty config", teacherId);

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
     * DELETE /api/availability/teacher/{teacherId}
     */
    @DeleteMapping("/teacher/{teacherId}")
    public ResponseEntity<Void> deleteTeacherAvailability(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId) {

        log.info("DELETE /api/availability/teacher/{} - Deleting availability", teacherId);

        // Authorization check
        if (!userId.equals(teacherId)) {
            log.error("Unauthorized: user {} trying to delete availability for teacher {}", userId, teacherId);
            throw new IllegalArgumentException("Unauthorized: You can only delete your own availability");
        }

        availabilityManagementService.deleteTeacherAvailability(teacherId);
        log.info("Successfully deleted availability for teacher: {}", teacherId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Add a single time slot
     * POST /api/availability/teacher/{teacherId}/slot
     */
    @PostMapping("/teacher/{teacherId}/slot")
    public ResponseEntity<TeacherAvailabilityDto> addTimeSlot(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestBody TimeSlot timeSlot) {

        log.info("Adding time slot for teacher {} on {}: {} - {}",
                teacherId, dayOfWeek, timeSlot.getStartTime(), timeSlot.getEndTime());

        if (!userId.equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        TeacherAvailabilityDto availability = availabilityManagementService.addTimeSlot(
                teacherId, dayOfWeek, timeSlot);

        return ResponseEntity.ok(availability);
    }

    /**
     * Remove a time slot
     * DELETE /api/availability/teacher/{teacherId}/slot
     */
    @DeleteMapping("/teacher/{teacherId}/slot")
    public ResponseEntity<TeacherAvailabilityDto> removeTimeSlot(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestBody TimeSlot timeSlot) {

        log.info("Removing time slot for teacher {} on {}: {} - {}",
                teacherId, dayOfWeek, timeSlot.getStartTime(), timeSlot.getEndTime());

        if (!userId.equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        TeacherAvailabilityDto availability = availabilityManagementService.removeTimeSlot(
                teacherId, dayOfWeek, timeSlot);

        return ResponseEntity.ok(availability);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Convert weeklyAvailability from Object map to proper DayOfWeek map
     * Handles both String keys (from JSON) and DayOfWeek keys
     */
    private Map<DayOfWeek, List<TimeSlot>> convertWeeklyAvailability(Object weeklyAvailabilityObj) {
        Map<DayOfWeek, List<TimeSlot>> result = new HashMap<>();

        if (weeklyAvailabilityObj == null) {
            return result;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMap = (Map<String, Object>) weeklyAvailabilityObj;

            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                try {
                    // Convert string day to DayOfWeek enum
                    DayOfWeek day = DayOfWeek.valueOf(entry.getKey().toUpperCase());

                    // Convert slots list
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> slotsList = (List<Map<String, Object>>) entry.getValue();

                    List<TimeSlot> timeSlots = slotsList.stream()
                            .map(this::convertToTimeSlot)
                            .toList();

                    result.put(day, timeSlots);

                } catch (IllegalArgumentException e) {
                    log.warn("Invalid day of week: {}", entry.getKey());
                }
            }

        } catch (Exception e) {
            log.error("Error converting weekly availability: {}", e.getMessage(), e);
            throw new RuntimeException("Invalid weekly availability format", e);
        }

        return result;
    }

    /**
     * Convert map to TimeSlot object
     */
    private TimeSlot convertToTimeSlot(Map<String, Object> slotMap) {
        String startTime = (String) slotMap.get("startTime");
        String endTime = (String) slotMap.get("endTime");
        Boolean isAvailable = slotMap.containsKey("isAvailable")
                ? (Boolean) slotMap.get("isAvailable")
                : true;

        return TimeSlot.builder()
                .startTime(startTime)
                .endTime(endTime)
                .isAvailable(isAvailable)
                .build();
    }

    // ==================== REQUEST/RESPONSE DTOs ====================

    /**
     * Request body for setting availability
     */
    @lombok.Data
    public static class AvailabilityRequest {
        private String teacherId;
        private String timezone;
        private Object weeklyAvailability; // Using Object to handle JSON deserialization
        private Integer bufferTimeMinutes;
        private Integer maxSessionsPerDay;
    }
}
