package com.tcon.learning_management_service.availability.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TimezoneValidationService {

    public String validateAndNormalizeTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return "UTC";
        }

        try {
            return ZoneId.of(timezone.trim()).getId();
        } catch (Exception e) {
            log.error("Invalid timezone received: {}", timezone);
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
    }

    public List<String> getAllAvailableTimezones() {
        return ZoneId.getAvailableZoneIds()
                .stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public String getCurrentOffset(String timezoneId) {
        ZoneOffset offset = ZonedDateTime.now(ZoneId.of(timezoneId)).getOffset();
        return offset.getId().replace("Z", "+00:00");
    }
}