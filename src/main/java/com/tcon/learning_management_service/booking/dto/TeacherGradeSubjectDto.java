package com.tcon.learning_management_service.booking.dto;

import lombok.*;

import java.util.List;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeacherGradeSubjectDto {


    private String grade;
    private List<String> subjects;
}