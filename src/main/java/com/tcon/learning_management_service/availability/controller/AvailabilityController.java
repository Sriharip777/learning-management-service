package com.tcon.learning_management_service.availability.controller;

import com.tcon.learning_management_service.availability.dto.BatchDateAvailabilityRequest;
import com.tcon.learning_management_service.availability.dto.DateSpecificAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.SessionMode;
import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDto;
import com.tcon.learning_management_service.availability.entity.WeeklyTimeSlot;
import com.tcon.learning_management_service.availability.service.AvailabilityManagementService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityManagementService availabilityManagementService;

    @PostMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> setTeacherAvailability(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody AvailabilityRequest request) {

        log.info("POST /api/availability/teacher/{} - Setting availability", teacherId);

        if (!userId.equals(teacherId)) {
            log.error("Unauthorized: user {} trying to set availability for teacher {}", userId, teacherId);
            throw new IllegalArgumentException("Unauthorized: You can only set your own availability");
        }

        try {
            Map<DayOfWeek, List<WeeklyTimeSlot>> weeklyAvailability =
                    convertWeeklyAvailability(request.getWeeklyAvailability());

            TeacherAvailabilityDto availability =
                    availabilityManagementService.setTeacherAvailability(
                            teacherId,
                            weeklyAvailability,
                            request.getBufferTimeMinutes(),
                            request.getMaxSessionsPerDay(),
                            request.getOneOnOneEnabled(),
                            request.getGroupEnabled(),
                            request.getWeeklyPattern()
                    );

            log.info("Successfully set availability for teacher: {}", teacherId);
            return ResponseEntity.ok(availability);

        } catch (Exception e) {
            log.error("Error setting availability for teacher {}: {}", teacherId, e.getMessage(), e);
            throw new RuntimeException("Failed to set availability: " + e.getMessage(), e);
        }
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> getTeacherAvailability(
            @PathVariable String teacherId) {

        log.info("GET /api/availability/teacher/{} - Getting availability", teacherId);

        try {
            TeacherAvailabilityDto availability =
                    availabilityManagementService.getTeacherAvailability(teacherId);

            log.info("Found availability for teacher {} with {} days configured",
                    teacherId,
                    availability.getWeeklyAvailability() != null
                            ? availability.getWeeklyAvailability().size()
                            : 0);

            return ResponseEntity.ok(availability);

        } catch (IllegalArgumentException e) {
            log.warn("Teacher availability not found: {} - Returning empty config", teacherId);

            return ResponseEntity.ok(
                    TeacherAvailabilityDto.builder()
                            .teacherId(teacherId)
                            .bufferTimeMinutes(15)
                            .weeklyAvailability(new HashMap<>())
                            .build()
            );
        }
    }

    @DeleteMapping("/teacher/{teacherId}")
    public ResponseEntity<Void> deleteTeacherAvailability(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId) {

        log.info("DELETE /api/availability/teacher/{} - Deleting availability", teacherId);

        if (!userId.equals(teacherId)) {
            log.error("Unauthorized: user {} trying to delete availability for teacher {}", userId, teacherId);
            throw new IllegalArgumentException("Unauthorized: You can only delete your own availability");
        }

        availabilityManagementService.deleteTeacherAvailability(teacherId);
        log.info("Successfully deleted availability for teacher: {}", teacherId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/teacher/{teacherId}/slot")
    public ResponseEntity<TeacherAvailabilityDto> addTimeSlot(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestBody WeeklyTimeSlot timeSlot) {

        log.info("Adding time slot for teacher {} on {}: {} - {}",
                teacherId, dayOfWeek, timeSlot.getStartTime(), timeSlot.getEndTime());

        if (!userId.equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        TeacherAvailabilityDto availability = availabilityManagementService.addTimeSlot(
                teacherId, dayOfWeek, timeSlot);

        return ResponseEntity.ok(availability);
    }

    @DeleteMapping("/teacher/{teacherId}/slot")
    public ResponseEntity<TeacherAvailabilityDto> removeTimeSlot(
            @PathVariable String teacherId,
            @RequestHeader("X-User-Id") String userId,
            @RequestParam DayOfWeek dayOfWeek,
            @RequestBody WeeklyTimeSlot timeSlot) {

        log.info("Removing time slot for teacher {} on {}: {} - {}",
                teacherId, dayOfWeek, timeSlot.getStartTime(), timeSlot.getEndTime());

        if (!userId.equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized");
        }

        TeacherAvailabilityDto availability = availabilityManagementService.removeTimeSlot(
                teacherId, dayOfWeek, timeSlot);

        return ResponseEntity.ok(availability);
    }

    @PostMapping("/date-specific/batch")
    public ResponseEntity<Map<String, Object>> saveDateSpecificAvailabilityBatch(
            @RequestBody BatchDateAvailabilityRequest request) {

        int count = request != null && request.getDateSlots() != null ? request.getDateSlots().size() : 0;
        log.info("Saving batch date-specific availability for teacher {}", request != null ? request.getTeacherId() : null);
        log.info("Date slots count: {}", count);

        try {
            availabilityManagementService.saveDateSpecificAvailabilityBatch(request);

            log.info("Saved {} date-specific availability entries", count);
            return ResponseEntity.ok(Map.of(
                    "message", "Availability saved successfully",
                    "count", count
            ));

        } catch (Exception e) {
            log.error("Failed to save date-specific availability", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Failed to save availability: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/date-specific/{teacherId}")
    public ResponseEntity<List<DateSpecificAvailabilityDto>> getDateSpecificAvailability(
            @PathVariable String teacherId,
            @RequestParam(required = false) SessionMode mode) {

        log.info("Fetching date-specific availability for teacher {} with mode {}", teacherId, mode);

        try {
            List<DateSpecificAvailabilityDto> availability =
                    availabilityManagementService.getDateSpecificAvailability(teacherId, mode);

            log.info("Found {} date-specific entries", availability.size());
            return ResponseEntity.ok(availability);

        } catch (Exception e) {
            log.error("Failed to fetch date-specific availability", e);
            return ResponseEntity.internalServerError().body(List.of());
        }
    }

    @DeleteMapping("/date-specific/{teacherId}/{dayStartUtc}")
    public ResponseEntity<Map<String, String>> deleteDateSpecificAvailability(
            @PathVariable String teacherId,
            @PathVariable String dayStartUtc) {

        log.info("Deleting date-specific availability for teacher {} on {}", teacherId, dayStartUtc);

        try {
            Instant utcDayStart = Instant.parse(dayStartUtc);
            availabilityManagementService.deleteDateSpecificAvailability(teacherId, utcDayStart);

            return ResponseEntity.ok(Map.of("message", "Availability deleted successfully"));

        } catch (Exception e) {
            log.error("Failed to delete date-specific availability", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "message", "Failed to delete availability: " + e.getMessage()
            ));
        }
    }

    private Map<DayOfWeek, List<WeeklyTimeSlot>> convertWeeklyAvailability(Object weeklyAvailabilityObj) {
        Map<DayOfWeek, List<WeeklyTimeSlot>> result = new HashMap<>();

        if (weeklyAvailabilityObj == null) {
            return result;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rawMap = (Map<String, Object>) weeklyAvailabilityObj;

            for (Map.Entry<String, Object> entry : rawMap.entrySet()) {
                try {
                    DayOfWeek day = DayOfWeek.valueOf(entry.getKey().toUpperCase());

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> slotsList = (List<Map<String, Object>>) entry.getValue();

                    List<WeeklyTimeSlot> timeSlots = slotsList.stream()
                            .map(this::convertToWeeklyTimeSlot)
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

    private WeeklyTimeSlot convertToWeeklyTimeSlot(Map<String, Object> slotMap) {
        String startTime = (String) slotMap.get("startTime");
        String endTime = (String) slotMap.get("endTime");
        Boolean isAvailable = slotMap.containsKey("isAvailable")
                ? (Boolean) slotMap.get("isAvailable")
                : true;

        String modeStr = (String) slotMap.getOrDefault("mode", "ONE_ON_ONE");
        SessionMode mode;
        try {
            mode = SessionMode.valueOf(modeStr);
        } catch (IllegalArgumentException ex) {
            mode = SessionMode.ONE_ON_ONE;
        }

        return WeeklyTimeSlot.builder()
                .startTime(startTime)
                .endTime(endTime)
                .isAvailable(isAvailable)
                .mode(mode)
                .build();
    }

    @Data
    public static class AvailabilityRequest {
        private String teacherId;
        private Object weeklyAvailability;
        private Integer bufferTimeMinutes;
        private Integer maxSessionsPerDay;
        private Boolean oneOnOneEnabled;
        private Boolean groupEnabled;
        private WeeklyPatternDto weeklyPattern;
    }
}