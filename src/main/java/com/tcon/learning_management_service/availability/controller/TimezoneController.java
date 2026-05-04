package com.tcon.learning_management_service.availability.controller;

import com.tcon.learning_management_service.availability.dto.SessionMode;
import com.tcon.learning_management_service.availability.dto.TimeSlotDisplayDto;
import com.tcon.learning_management_service.availability.dto.TimezoneResponseDto;
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
import java.util.LinkedHashMap;
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

    @GetMapping
    public ResponseEntity<List<TimezoneResponseDto>> getAllTimezones() {
        log.info("Fetching all global timezones");
        List<TimezoneResponseDto> timezones = timezoneService.getAllTimezones();
        log.info("Returning {} timezone entries", timezones.size());
        return ResponseEntity.ok(timezones);
    }

    @GetMapping("/details")
    public ResponseEntity<TimezoneResponseDto> getTimezoneDetails(
            @RequestParam String timezone) {

        log.info("Fetching timezone details for {}", timezone);
        return ResponseEntity.ok(timezoneService.getTimezoneDetails(timezone));
    }

    @GetMapping("/date-specific/{teacherId}")
    public ResponseEntity<Map<String, List<TimeSlotDisplayDto>>> getDateSpecificWithTimezone(
            @PathVariable String teacherId,
            @RequestParam String timezone,
            @RequestParam(required = false) SessionMode mode) {

        log.info("Fetching timezone-aware date-specific slots for teacher {} in timezone {}",
                teacherId, timezone);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusMonths(6);

        Map<String, List<TimeSlotDisplayDto>> result =
                dateSpecificRepository
                        .findByTeacherIdAndDateBetween(teacherId, today, futureDate)
                        .stream()
                        .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
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

                                    String sourceTimezone = avail.getTimezone() != null
                                            ? avail.getTimezone()
                                            : "UTC";

                                    return timezoneService.convertSlotsToTimezone(
                                            slots,
                                            sourceTimezone,
                                            timezone,
                                            avail.getDate()
                                    );
                                },
                                (existing, replacement) -> existing,
                                LinkedHashMap::new
                        ));

        log.info("Returning converted date-specific slots for {} dates", result.size());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/weekly/{teacherId}")
    public ResponseEntity<Map<String, List<TimeSlotDisplayDto>>> getWeeklyWithTimezone(
            @PathVariable String teacherId,
            @RequestParam String timezone) {

        log.info("Fetching timezone-aware weekly slots for teacher {} in timezone {}",
                teacherId, timezone);

        return teacherAvailabilityRepository.findByTeacherId(teacherId)
                .map(availability -> {
                    String sourceTimezone = availability.getTimezone() != null
                            ? availability.getTimezone()
                            : "UTC";

                    Map<String, List<TimeSlotDisplayDto>> result =
                            availability.getWeeklyAvailability().entrySet().stream()
                                    .collect(Collectors.toMap(
                                            entry -> entry.getKey().name(),
                                            entry -> timezoneService.convertSlotsToTimezone(
                                                    entry.getValue(),
                                                    sourceTimezone,
                                                    timezone,
                                                    resolveNextDate(entry.getKey())
                                            ),
                                            (existing, replacement) -> existing,
                                            LinkedHashMap::new
                                    ));

                    log.info("Returning weekly slots for {} days", result.size());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.ok(Map.of()));
    }

    @GetMapping("/weekly-pattern/{teacherId}")
    public ResponseEntity<WeeklyPatternDisplayDto> getWeeklyPatternWithTimezone(
            @PathVariable String teacherId,
            @RequestParam String timezone) {

        log.info("Fetching weekly pattern for teacher {} in timezone {}",
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

                    log.info("Returning weekly pattern for teacher {}", teacherId);
                    return ResponseEntity.ok(display);
                })
                .orElse(ResponseEntity.ok(
                        WeeklyPatternDisplayDto.builder()
                                .enabled(false)
                                .days(List.of())
                                .dayNames(List.of())
                                .timezoneId(timezone)
                                .build()
                ));
    }

    private LocalDate resolveNextDate(java.time.DayOfWeek dayOfWeek) {
        LocalDate today = LocalDate.now();
        int diff = dayOfWeek.getValue() - today.getDayOfWeek().getValue();

        if (diff < 0) {
            diff += 7;
        }

        return today.plusDays(diff);
    }
}