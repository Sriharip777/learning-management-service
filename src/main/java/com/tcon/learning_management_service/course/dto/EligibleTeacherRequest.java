// package at top must match imports everywhere
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
public class EligibleTeacherRequest {
    private String gradeId;
    private String subjectId;
    private List<String> topicIds;
}