package com.tcon.learning_management_service.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableTeacherDto {
    private String id;
    private String name;
    private String avatar;
    private Double hourlyRate;
    private String currency;
    private Double rating;
    private List<String> subjects;
}