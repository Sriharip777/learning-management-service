package com.tcon.learning_management_service.availability.controller;
import com.tcon.learning_management_service.availability.dto.TeacherAvailabilityDto;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.service.AvailabilityManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityManagementService availabilityService;

    @PostMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> setTeacherAvailability(
            @PathVariable String teacherId,
            @RequestBody Map<String, Object> availabilityData) {

        @SuppressWarnings("unchecked")
        Map<DayOfWeek, List<TimeSlot>> weeklyAvailability =
                (Map<DayOfWeek, List<TimeSlot>>) availabilityData.get("weeklyAvailability");

        String timezone = (String) availabilityData.get("timezone");
        Integer bufferTimeMinutes = (Integer) availabilityData.get("bufferTimeMinutes");
        Integer maxSessionsPerDay = (Integer) availabilityData.get("maxSessionsPerDay");

        TeacherAvailabilityDto dto = availabilityService.setTeacherAvailability(
                teacherId, weeklyAvailability, timezone, bufferTimeMinutes, maxSessionsPerDay);

        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<TeacherAvailabilityDto> getTeacherAvailability(@PathVariable String teacherId) {
        TeacherAvailabilityDto dto = availabilityService.getTeacherAvailability(teacherId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/teacher/{teacherId}/slot")
    public ResponseEntity<TeacherAvailabilityDto> addTimeSlot(
            @PathVariable String teacherId,
            @RequestBody Map<String, Object> slotData) {

        DayOfWeek dayOfWeek = DayOfWeek.valueOf((String) slotData.get("dayOfWeek"));

        @SuppressWarnings("unchecked")
        Map<String, String> timeSlotData = (Map<String, String>) slotData.get("timeSlot");

        TimeSlot timeSlot = TimeSlot.builder()
                .startTime(java.time.LocalTime.parse(timeSlotData.get("startTime")))
                .endTime(java.time.LocalTime.parse(timeSlotData.get("endTime")))
                .isAvailable(true)
                .build();

        TeacherAvailabilityDto dto = availabilityService.addTimeSlot(teacherId, dayOfWeek, timeSlot);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/teacher/{teacherId}/slot")
    public ResponseEntity<TeacherAvailabilityDto> removeTimeSlot(
            @PathVariable String teacherId,
            @RequestBody Map<String, Object> slotData) {

        DayOfWeek dayOfWeek = DayOfWeek.valueOf((String) slotData.get("dayOfWeek"));

        @SuppressWarnings("unchecked")
        Map<String, String> timeSlotData = (Map<String, String>) slotData.get("timeSlot");

        TimeSlot timeSlot = TimeSlot.builder()
                .startTime(java.time.LocalTime.parse(timeSlotData.get("startTime")))
                .endTime(java.time.LocalTime.parse(timeSlotData.get("endTime")))
                .build();

        TeacherAvailabilityDto dto = availabilityService.removeTimeSlot(teacherId, dayOfWeek, timeSlot);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/teacher/{teacherId}")
    public ResponseEntity<Map<String, String>> deleteTeacherAvailability(@PathVariable String teacherId) {
        availabilityService.deleteTeacherAvailability(teacherId);
        return ResponseEntity.ok(Map.of("message", "Availability deleted successfully"));
    }
}
