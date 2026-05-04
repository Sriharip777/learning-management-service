package com.tcon.learning_management_service.availability.service;

import com.tcon.learning_management_service.availability.dto.TimeSlotDisplayDto;
import com.tcon.learning_management_service.availability.dto.TimezoneResponseDto;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDisplayDto;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimezoneService {

    private static final DateTimeFormatter DISPLAY_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a");

    private static final DateTimeFormatter DISPLAY_TIME_WITH_ZONE_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a z");

    private static final DateTimeFormatter TIMEZONE_ABBR_FORMATTER =
            DateTimeFormatter.ofPattern("z");

    private static final DateTimeFormatter OFFSET_FORMATTER =
            DateTimeFormatter.ofPattern("XXX");

    private static final DateTimeFormatter FLEXIBLE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm[:ss]");

    private final TimezoneValidationService timezoneValidationService;

    public List<TimezoneResponseDto> getAllTimezones() {
        return timezoneValidationService.getAllAvailableTimezones()
                .stream()
                .map(this::buildTimezoneResponse)
                .collect(Collectors.toList());
    }

    public TimezoneResponseDto getTimezoneDetails(String timezoneId) {
        String validatedTimezone = timezoneValidationService.validateAndNormalizeTimezone(timezoneId);
        return buildTimezoneResponse(validatedTimezone);
    }

    public List<TimeSlotDisplayDto> convertSlotsToTimezone(
            List<TimeSlot> slots,
            String targetTimezoneId) {

        String validatedTargetTimezone = timezoneValidationService.validateAndNormalizeTimezone(targetTimezoneId);
        ZoneId targetZone = ZoneId.of(validatedTargetTimezone);

        return slots.stream()
                .map(slot -> convertSingleSlot(slot, targetZone, validatedTargetTimezone))
                .collect(Collectors.toList());
    }

    public WeeklyPatternDisplayDto convertPatternToTimezone(
            String timeStart,
            String timeEnd,
            Boolean enabled,
            List<Integer> days,
            String storedTimezoneId,
            String targetTimezoneId) {

        String validatedSourceTimezone = timezoneValidationService.validateAndNormalizeTimezone(storedTimezoneId);
        String validatedTargetTimezone = timezoneValidationService.validateAndNormalizeTimezone(targetTimezoneId);

        ZoneId sourceZone = ZoneId.of(validatedSourceTimezone);
        ZoneId targetZone = ZoneId.of(validatedTargetTimezone);

        String displayStart = timeStart;
        String displayEnd = timeEnd;
        String timezoneAbbreviation = "";
        String utcOffset = "";

        try {
            if (timeStart != null && !timeStart.isBlank() && timeEnd != null && !timeEnd.isBlank()) {
                LocalTime start = LocalTime.parse(timeStart, FLEXIBLE_TIME_FORMATTER);
                LocalTime end = LocalTime.parse(timeEnd, FLEXIBLE_TIME_FORMATTER);

                ZonedDateTime startConverted = ZonedDateTime.of(LocalDate.now(), start, sourceZone)
                        .withZoneSameInstant(targetZone);

                ZonedDateTime endConverted = ZonedDateTime.of(LocalDate.now(), end, sourceZone)
                        .withZoneSameInstant(targetZone);

                displayStart = startConverted.format(DISPLAY_TIME_FORMATTER);
                displayEnd = endConverted.format(DISPLAY_TIME_FORMATTER);
                timezoneAbbreviation = startConverted.format(TIMEZONE_ABBR_FORMATTER);
                utcOffset = startConverted.format(OFFSET_FORMATTER);
            }
        } catch (Exception e) {
            log.error("Error converting weekly pattern from {} to {}: {}",
                    validatedSourceTimezone, validatedTargetTimezone, e.getMessage());
        }

        return WeeklyPatternDisplayDto.builder()
                .enabled(enabled)
                .days(days != null ? days : List.of())
                .dayNames(resolveDayNames(days))
                .timeStart(timeStart)
                .timeEnd(timeEnd)
                .displayTimeStart(displayStart)
                .displayTimeEnd(displayEnd)
                .timezoneAbbreviation(timezoneAbbreviation)
                .timezoneId(validatedTargetTimezone)
                .utcOffset(utcOffset)
                .build();
    }

    private TimezoneResponseDto buildTimezoneResponse(String timezoneId) {
        ZoneId zoneId = ZoneId.of(timezoneId);
        ZonedDateTime now = ZonedDateTime.now(zoneId);

        return TimezoneResponseDto.builder()
                .timezoneId(timezoneId)
                .displayName(timezoneId)
                .currentTime(now.format(DISPLAY_TIME_WITH_ZONE_FORMATTER))
                .currentOffset(now.format(OFFSET_FORMATTER))
                .timezoneAbbreviation(now.format(TIMEZONE_ABBR_FORMATTER))
                .build();
    }

    private TimeSlotDisplayDto convertSingleSlot(
            TimeSlot slot,
            ZoneId targetZone,
            String timezoneId) {

        try {
            LocalTime start = LocalTime.parse(slot.getStartTime(), FLEXIBLE_TIME_FORMATTER);
            LocalTime end = LocalTime.parse(slot.getEndTime(), FLEXIBLE_TIME_FORMATTER);

            ZonedDateTime startConverted = ZonedDateTime.of(LocalDate.now(), start, targetZone);
            ZonedDateTime endConverted = ZonedDateTime.of(LocalDate.now(), end, targetZone);

            return TimeSlotDisplayDto.builder()
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .isAvailable(slot.getIsAvailable())
                    .mode(slot.getMode())
                    .displayStartTime(startConverted.format(DISPLAY_TIME_FORMATTER))
                    .displayEndTime(endConverted.format(DISPLAY_TIME_FORMATTER))
                    .timezoneAbbreviation(startConverted.format(TIMEZONE_ABBR_FORMATTER))
                    .timezoneId(timezoneId)
                    .utcOffset(startConverted.format(OFFSET_FORMATTER))
                    .build();

        } catch (Exception e) {
            log.error("Error converting time slot {} - {}: {}",
                    slot.getStartTime(), slot.getEndTime(), e.getMessage());

            return TimeSlotDisplayDto.builder()
                    .startTime(slot.getStartTime())
                    .endTime(slot.getEndTime())
                    .isAvailable(slot.getIsAvailable())
                    .mode(slot.getMode())
                    .displayStartTime(slot.getStartTime())
                    .displayEndTime(slot.getEndTime())
                    .timezoneAbbreviation("")
                    .timezoneId(timezoneId)
                    .utcOffset("")
                    .build();
        }
    }

    private List<String> resolveDayNames(List<Integer> days) {
        String[] names = {"SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY"};

        if (days == null) {
            return List.of();
        }

        return days.stream()
                .filter(day -> day != null && day >= 0 && day <= 6)
                .map(day -> names[day])
                .collect(Collectors.toList());
    }
}