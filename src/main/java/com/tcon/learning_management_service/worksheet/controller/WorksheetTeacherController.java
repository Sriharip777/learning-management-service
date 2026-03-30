package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.AssignWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.integration.AssignmentIntegrationService;
import com.tcon.learning_management_service.worksheet.service.WorksheetQueryService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/worksheets")
@RequiredArgsConstructor
public class WorksheetTeacherController {

    private final WorksheetQueryService queryService;
    private final AssignmentIntegrationService assignmentIntegrationService;

    /*
     * GET AVAILABLE WORKSHEETS
     */
    @GetMapping
    public List<WorksheetSummaryResponse> getWorksheets(
            @RequestParam String subjectId,
            @RequestParam String gradeId,
            @RequestParam(required = false) String topicId
    ) {
        return queryService.getPublishedWorksheets(
                subjectId,
                gradeId,
                topicId
        );
    }

    /*
     * ASSIGN WORKSHEET
     */
    @PostMapping("/{worksheetId}/assign")
    public String assignWorksheet(
            @PathVariable String worksheetId,
            @RequestBody AssignWorksheetRequest request
    ) {
        assignmentIntegrationService.assignWorksheet(
                worksheetId,
                request.getTeacherId(),
                request.getStudentIds(),
                request.getDueDate()
        );

        return "Worksheet assigned successfully";
    }
}