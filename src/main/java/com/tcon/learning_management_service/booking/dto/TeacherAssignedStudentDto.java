package com.tcon.learning_management_service.booking.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherAssignedStudentDto {
    private String userId;
    private String studentId;
    private String name;
    private String email;
}