package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.integration.AssignmentIntegrationService;
import com.tcon.learning_management_service.worksheet.service.WorksheetQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/teacher/worksheets")
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
            @RequestParam String gradeId
    ) {
        return queryService.getPublishedWorksheets(
                subjectId,
                gradeId
        );
    }

    /*
     * ASSIGN WORKSHEET
     */
    @PostMapping("/{worksheetId}/assign")
    public void assignWorksheet(
            @PathVariable String worksheetId
    ) {
        assignmentIntegrationService.assignWorksheet(
                worksheetId
        );
    }
}