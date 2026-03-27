package com.tcon.learning_management_service.worksheet.integration;

import com.tcon.learning_management_service.worksheet.integration.dto.GradeDto;
import com.tcon.learning_management_service.worksheet.integration.dto.SubjectDto;
import com.tcon.learning_management_service.worksheet.integration.dto.TopicDto;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "learning-management-service",
        path = "/api"   // ✅ required
)
public interface CourseClient {

    /**
     * Get all grades
     */
    @GetMapping("/grades")
    List<GradeDto> getGrades();

    /**
     * Get subjects by gradeId
     */
    @GetMapping("/grades/{gradeId}/subjects")
    List<SubjectDto> getSubjectsByGrade(
            @PathVariable("gradeId") String gradeId
    );

    /**
     * Get topics by subjectId
     */
    @GetMapping("/subjects/{subjectId}/topics")
    List<TopicDto> getTopicsBySubject(
            @PathVariable("subjectId") String subjectId
    );
}