package com.tcon.learning_management_service.availability.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "date_specific_availability")
@CompoundIndex(name = "teacher_date_idx", def = "{'teacherId': 1, 'date': 1}", unique = true)
public class DateSpecificAvailability {

    @Id
    private String id;

    private String teacherId;

    private LocalDate date; // Specific date (e.g., 2026-02-15)

    @Builder.Default
    private List<TimeSlot> timeSlots = new ArrayList<>();

    private String timezone;

    private Integer bufferTimeMinutes;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}