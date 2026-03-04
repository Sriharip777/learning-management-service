package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.AddQuestionRequest;
import com.tcon.learning_management_service.worksheet.dto.request.CreateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.request.UpdateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResponse;
import com.tcon.learning_management_service.worksheet.service.WorksheetPublishService;
import com.tcon.learning_management_service.worksheet.service.WorksheetService;
import com.tcon.learning_management_service.worksheet.service.WorksheetVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/worksheets")
@RequiredArgsConstructor
public class WorksheetAdminController {

    private final WorksheetService worksheetService;
    private final WorksheetVersionService versionService;
    private final WorksheetPublishService publishService;

    /*
     * CREATE WORKSHEET
     */
    @PostMapping
    public WorksheetResponse createWorksheet(
            @RequestBody CreateWorksheetRequest request
    ) {
        // createdBy normally comes from JWT
        return worksheetService.createWorksheet(request, "admin");
    }

    /*
     * UPDATE WORKSHEET
     */
    @PutMapping
    public WorksheetResponse updateWorksheet(
            @RequestBody UpdateWorksheetRequest request
    ) {
        return worksheetService.updateWorksheet(request);
    }

    /*
     * ADD QUESTIONS
     */
    @PostMapping("/questions")
    public void addQuestions(
            @RequestBody AddQuestionRequest request
    ) {
        versionService.addQuestions(request);
    }

    /*
     * PUBLISH WORKSHEET
     */
    @PostMapping("/{worksheetId}/publish")
    public void publishWorksheet(
            @PathVariable String worksheetId
    ) {
        publishService.publishWorksheet(worksheetId);
    }
}