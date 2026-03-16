package com.tcon.learning_management_service.availability.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlotDisplayDto {

    // Original stored UTC times
    private String startTime;           // "10:00"
    private String endTime;             // "11:00"
    private Boolean isAvailable;
    private SessionMode mode;

    // Timezone-converted display times
    private String displayStartTime;    // "05:30 AM EST"
    private String displayEndTime;      // "06:30 AM EST"
    private String timezoneAbbreviation;// "EST"
    private String timezoneId;          // "America/New_York"
    private String stateName;           // "New York"
    private String stateCode;           // "NY"
}
