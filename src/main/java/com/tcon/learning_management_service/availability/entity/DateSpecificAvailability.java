package com.tcon.learning_management_service.availability.entity;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "date_specific_availability")
@CompoundIndex(name = "teacher_day_idx", def = "{'teacherId': 1, 'dayStartUtc': 1}", unique = true)
public class DateSpecificAvailability {

    @Id
    private String id;

    private String teacherId;

    // Start of the UTC day for this availability group
    private Instant dayStartUtc;

    @Builder.Default
    private List<AvailabilitySlot> slots = new ArrayList<>();

    private Integer bufferTimeMinutes;

    private Boolean oneOnOneEnabled;

    private Boolean groupEnabled;

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}