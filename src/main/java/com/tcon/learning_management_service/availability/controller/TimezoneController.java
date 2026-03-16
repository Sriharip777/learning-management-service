package com.tcon.learning_management_service.availability.controller;

import com.tcon.learning_management_service.availability.dto.SessionMode;
import com.tcon.learning_management_service.availability.dto.TimezoneResponseDto;
import com.tcon.learning_management_service.availability.dto.TimeSlotDisplayDto;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDisplayDto;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import com.tcon.learning_management_service.availability.repository.DateSpecificAvailabilityRepository;
import com.tcon.learning_management_service.availability.repository.TeacherAvailabilityRepository;
import com.tcon.learning_management_service.availability.service.TimezoneService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/availability/timezones")
@RequiredArgsConstructor
public class TimezoneController {

    private final TimezoneService timezoneService;
    private final DateSpecificAvailabilityRepository dateSpecificRepository;
    private final TeacherAvailabilityRepository teacherAvailabilityRepository;

    @GetMapping("/usa")
    public ResponseEntity<List<TimezoneResponseDto>> getAllUsaTimezones() {
        log.info("📋 Fetching all USA state timezones");
        List<TimezoneResponseDto> timezones = timezoneService.getAllUsaTimezones();
        log.info("✅ Returning {} timezone entries", timezones.size());
        return ResponseEntity.ok(timezones);
    }

    @GetMapping("/date-specific/{teacherId}")
    public ResponseEntity<Map<String, List<TimeSlotDisplayDto>>> getDateSpecificWithTimezone(
            @PathVariable String teacherId,
            @RequestParam String timezone,
            @RequestParam(required = false) SessionMode mode) {

        log.info("📅 Fetching timezone-aware slots for teacher {} in timezone {}",
                teacherId, timezone);

        LocalDate today      = LocalDate.now();
        LocalDate futureDate = today.plusMonths(6);

        Map<String, List<TimeSlotDisplayDto>> result =
                dateSpecificRepository
                        .findByTeacherIdAndDateBetween(teacherId, today, futureDate)
                        .stream()
                        .collect(Collectors.toMap(
                                avail -> avail.getDate().toString(),
                                avail -> {
                                    List<TimeSlot> slots = avail.getTimeSlots();
                                    if (mode != null) {
                                        slots = slots.stream()
                                                .filter(s ->
                                                        (mode == SessionMode.ONE_ON_ONE && s.getMode() == null)
                                                                || s.getMode() == mode)
                                                .collect(Collectors.toList());
                                    }
                                    return timezoneService.convertSlotsToTimezone(slots, timezone);
                                }
                        ));

        log.info("✅ Returning timezone-converted slots for {} dates", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/weekly/{teacherId}")
    public ResponseEntity<Map<String, List<TimeSlotDisplayDto>>> getWeeklyWithTimezone(
            @PathVariable String teacherId,
            @RequestParam String timezone) {

        log.info("📅 Fetching timezone-aware WEEKLY slots for teacher {} in timezone {}",
                teacherId, timezone);

        return teacherAvailabilityRepository.findByTeacherId(teacherId)
                .map(availability -> {
                    Map<String, List<TimeSlotDisplayDto>> result =
                            availability.getWeeklyAvailability().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            entry -> entry.getKey().name(),
                                            entry -> timezoneService.convertSlotsToTimezone(
                                                    entry.getValue(), timezone)
                                    ));

                    log.info("✅ Returning weekly slots for {} days", result.size());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.ok(Map.of()));
    }

    /**
     * NEW: get weekly pattern converted to selected timezone
     */
    @GetMapping("/weekly-pattern/{teacherId}")
    public ResponseEntity<WeeklyPatternDisplayDto> getWeeklyPatternWithTimezone(
            @PathVariable String teacherId,
            @RequestParam String timezone) {

        log.info("📅 Fetching weekly pattern for teacher {} in timezone {}",
                teacherId, timezone);

        return teacherAvailabilityRepository.findByTeacherId(teacherId)
                .map(availability -> {
                    WeeklyPatternDisplayDto display =
                            timezoneService.convertPatternToTimezone(
                                    availability.getWeeklyPatternStart(),
                                    availability.getWeeklyPatternEnd(),
                                    availability.getWeeklyPatternEnabled(),
                                    availability.getWeeklyPatternDays(),
                                    availability.getTimezone(),
                                    timezone
                            );
                    log.info("✅ Returning weekly pattern display for teacher {}", teacherId);
                    return ResponseEntity.ok(display);
                })
                .orElse(ResponseEntity.ok(
                        WeeklyPatternDisplayDto.builder()
                                .enabled(false)
                                .days(List.of())
                                .dayNames(List.of())
                                .build()
                ));
    }
}
