package com.tcon.learning_management_service.availability.entity;
import com.tcon.learning_management_service.availability.dto.SessionMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "teacher_availability")
public class TeacherAvailability {

    @Id
    private String id;

    @Indexed(unique = true)
    private String teacherId;

    private String timezone;

    @Builder.Default
    private Map<DayOfWeek, List<TimeSlot>> weeklyAvailability = new java.util.HashMap<>();

    @Builder.Default
    private List<DateOverride> dateOverrides = new ArrayList<>();

    @Builder.Default
    private Integer bufferTimeMinutes = 15;

    private Integer maxSessionsPerDay;

    private Boolean oneOnOneEnabled;
    private Boolean groupEnabled;

    // ✅ CHANGED: multiple days instead of day1/day2 only
    @Builder.Default
    private List<Integer> weeklyPatternDays = new ArrayList<>(); // 0=Sun,1=Mon,...6=Sat

    // ✅ KEPT same
    private String weeklyPatternStart;    // "HH:mm"
    private String weeklyPatternEnd;      // "HH:mm"
    private Boolean weeklyPatternEnabled;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DateOverride {
        private LocalDateTime date;
        private Boolean isAvailable;
        private List<TimeSlot> timeSlots;
        private String reason;
    }
}
