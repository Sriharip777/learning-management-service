package com.tcon.learning_management_service.availability.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.*;

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

    @Builder.Default
    private Map<DayOfWeek, List<WeeklyTimeSlot>> weeklyAvailability = new HashMap<>();

    @Builder.Default
    private Integer bufferTimeMinutes = 15;

    private Integer maxSessionsPerDay;

    private Boolean oneOnOneEnabled;
    private Boolean groupEnabled;

    // Weekly pattern config (local time)
    @Builder.Default
    private List<Integer> weeklyPatternDays = new ArrayList<>(); // 0=Sun..6=Sat
    private String weeklyPatternStart; // "HH:mm[:ss]", normalized to "HH:mm:ss"
    private String weeklyPatternEnd;   // "HH:mm[:ss]", normalized to "HH:mm:ss"
    private Boolean weeklyPatternEnabled;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}