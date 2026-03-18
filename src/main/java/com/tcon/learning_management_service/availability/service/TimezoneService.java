package com.tcon.learning_management_service.availability.service;

import com.tcon.learning_management_service.availability.dto.TimezoneResponseDto;
import com.tcon.learning_management_service.availability.dto.TimeSlotDisplayDto;
import com.tcon.learning_management_service.availability.dto.UsaTimezone;
import com.tcon.learning_management_service.availability.dto.WeeklyPatternDisplayDto;
import com.tcon.learning_management_service.availability.entity.TimeSlot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TimezoneService {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("hh:mm a z");

    private static final DateTimeFormatter STORE_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Returns all USA state timezones for frontend dropdown
     */
    public List<TimezoneResponseDto> getAllUsaTimezones() {
        return Arrays.stream(UsaTimezone.values())
                .flatMap(tz -> {
                    try {
                        ZoneId zoneId = ZoneId.of(tz.getTimezoneId());
                        ZonedDateTime now = ZonedDateTime.now(zoneId);

                        TimezoneResponseDto dto = TimezoneResponseDto.builder()
                                .stateName(tz.getStateName())
                                .stateCode(tz.getStateCode())
                                .timezoneId(tz.getTimezoneId())
                                .timezoneLabel(tz.getTimezoneLabel())
                                .utcOffset(tz.getUtcOffset())
                                .currentTime(now.format(DISPLAY_FORMATTER))
                                .build();

                        return java.util.stream.Stream.of(dto);
                    } catch (Exception e) {
                        log.error("Invalid timezoneId '{}' for enum {}: {}",
                                tz.getTimezoneId(), tz.name(), e.getMessage());
                        return java.util.stream.Stream.<TimezoneResponseDto>empty();
                    }
                })
                .collect(Collectors.toList());
    }


    /**
     * Convert a list of TimeSlots to display format in the given timezone
     * stored times are in "HH:mm" (optionally with seconds) in UTC
     */
    public List<TimeSlotDisplayDto> convertSlotsToTimezone(
            List<TimeSlot> slots,
            String targetTimezoneId) {

        ZoneId targetZone = ZoneId.of(targetTimezoneId);
        ZoneId utcZone    = ZoneId.of("UTC");

        UsaTimezone usaTimezone = null;
        try {
            usaTimezone = UsaTimezone.findByTimezoneId(targetTimezoneId);
        } catch (IllegalArgumentException e) {
            log.warn("Not a USA timezone: {}", targetTimezoneId);
        }
        final UsaTimezone finalUsaTimezone = usaTimezone;

        DateTimeFormatter parseTimeFormatter =
                DateTimeFormatter.ofPattern("HH:mm[:ss]");

        return slots.stream().map(slot -> {
            try {
                LocalTime startUtc = LocalTime.parse(slot.getStartTime(), parseTimeFormatter);
                LocalTime endUtc   = LocalTime.parse(slot.getEndTime(),   parseTimeFormatter);

                ZonedDateTime startConverted = ZonedDateTime.of(
                                LocalDate.now(), startUtc, utcZone)
                        .withZoneSameInstant(targetZone);

                ZonedDateTime endConverted = ZonedDateTime.of(
                                LocalDate.now(), endUtc, utcZone)
                        .withZoneSameInstant(targetZone);

                return TimeSlotDisplayDto.builder()
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .isAvailable(slot.getIsAvailable())
                        .mode(slot.getMode())
                        .displayStartTime(startConverted.format(DateTimeFormatter.ofPattern("hh:mm a")))
                        .displayEndTime(endConverted.format(DateTimeFormatter.ofPattern("hh:mm a")))
                        .timezoneAbbreviation(startConverted.format(DateTimeFormatter.ofPattern("z")))
                        .timezoneId(targetTimezoneId)
                        .stateName(finalUsaTimezone != null ? finalUsaTimezone.getStateName() : "")
                        .stateCode(finalUsaTimezone != null ? finalUsaTimezone.getStateCode() : "")
                        .build();

            } catch (Exception e) {
                log.error("Error converting slot {} - {}: {}",
                        slot.getStartTime(), slot.getEndTime(), e.getMessage());

                return TimeSlotDisplayDto.builder()
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .isAvailable(slot.getIsAvailable())
                        .mode(slot.getMode())
                        .displayStartTime(slot.getStartTime())
                        .displayEndTime(slot.getEndTime())
                        .timezoneId(targetTimezoneId)
                        .build();
            }
        }).collect(Collectors.toList());
    }

    /**
     * Convert weekly pattern start/end times to viewer's selected timezone
     */
    public WeeklyPatternDisplayDto convertPatternToTimezone(
            String timeStart,
            String timeEnd,
            Boolean enabled,
            List<Integer> days,
            String storedTimezoneId,
            String targetTimezoneId) {

        ZoneId sourceZone = ZoneId.of(storedTimezoneId != null ? storedTimezoneId : "UTC");
        ZoneId targetZone = ZoneId.of(targetTimezoneId);

        String[] dayNames = {"SUNDAY","MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY"};

        List<String> selectedDayNames = new java.util.ArrayList<>();
        if (days != null) {
            for (Integer d : days) {
                if (d != null && d >= 0 && d <= 6) {
                    selectedDayNames.add(dayNames[d]);
                }
            }
        }

        String displayStart = timeStart;
        String displayEnd   = timeEnd;
        String tzAbbr       = "";

        try {
            if (timeStart != null && timeEnd != null) {
                LocalTime startTime = LocalTime.parse(timeStart, STORE_FORMATTER);
                LocalTime endTime   = LocalTime.parse(timeEnd,   STORE_FORMATTER);

                ZonedDateTime startConverted = ZonedDateTime.of(
                                LocalDate.now(), startTime, sourceZone)
                        .withZoneSameInstant(targetZone);
                ZonedDateTime endConverted = ZonedDateTime.of(
                                LocalDate.now(), endTime, sourceZone)
                        .withZoneSameInstant(targetZone);

                displayStart = startConverted.format(DateTimeFormatter.ofPattern("hh:mm a"));
                displayEnd   = endConverted.format(DateTimeFormatter.ofPattern("hh:mm a"));
                tzAbbr       = startConverted.format(DateTimeFormatter.ofPattern("z"));
            }
        } catch (Exception e) {
            log.error("Pattern time conversion error: {}", e.getMessage());
        }

        UsaTimezone usaTimezone = null;
        try {
            usaTimezone = UsaTimezone.findByTimezoneId(targetTimezoneId);
        } catch (IllegalArgumentException e) {
            log.warn("Not a USA timezone: {}", targetTimezoneId);
        }

        return WeeklyPatternDisplayDto.builder()
                .enabled(enabled)
                .days(days)
                .dayNames(selectedDayNames)
                .timeStart(timeStart)
                .timeEnd(timeEnd)
                .displayTimeStart(displayStart)
                .displayTimeEnd(displayEnd)
                .timezoneAbbreviation(tzAbbr)
                .timezoneId(targetTimezoneId)
                .stateName(usaTimezone != null ? usaTimezone.getStateName() : "")
                .stateCode(usaTimezone != null ? usaTimezone.getStateCode() : "")
                .utcOffset(usaTimezone != null ? usaTimezone.getUtcOffset() : "")
                .build();
    }
}