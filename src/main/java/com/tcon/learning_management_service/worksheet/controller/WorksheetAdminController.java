package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.AddQuestionRequest;
import com.tcon.learning_management_service.worksheet.dto.request.CreateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.request.UpdateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.UploadResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResponse;
import com.tcon.learning_management_service.worksheet.service.WorksheetPublishService;
import com.tcon.learning_management_service.worksheet.service.WorksheetService;
import com.tcon.learning_management_service.worksheet.service.WorksheetVersionService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/worksheets")
@RequiredArgsConstructor
public class WorksheetAdminController {

    private final WorksheetService worksheetService;
    private final WorksheetVersionService versionService;
    private final WorksheetPublishService publishService;

    /*
     * =====================================
     * CREATE WORKSHEET
     * =====================================
     */
    @PostMapping
    public WorksheetResponse createWorksheet(
            @RequestBody CreateWorksheetRequest request
    ) {
        // createdBy normally comes from JWT
        return worksheetService.createWorksheet(request, "admin");
    }

    /*
     * =====================================
     * UPDATE WORKSHEET
     * =====================================
     */
    @PutMapping
    public WorksheetResponse updateWorksheet(
            @RequestBody UpdateWorksheetRequest request
    ) {
        return worksheetService.updateWorksheet(request);
    }

    /*
     * =====================================
     * ADD QUESTIONS (MANUAL)
     * =====================================
     */
    @PostMapping("/{worksheetId}/questions")
    public void addQuestions(
            @PathVariable String worksheetId,
            @RequestBody AddQuestionRequest request
    ) {
        versionService.addQuestions(worksheetId, request);
    }

    /*
     * =====================================
     * UPLOAD QUESTIONS (EXCEL) 🔥
     * =====================================
     */
    @PostMapping("/{worksheetId}/upload-excel")
    public UploadResponse uploadQuestionsFromExcel(
            @PathVariable String worksheetId,
            @RequestParam("file") MultipartFile file
    ) {
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new RuntimeException("Only Excel (.xlsx) files are allowed");
        }

        return worksheetService.uploadQuestionsFromExcel(worksheetId, file);
    }

    /*
     * =====================================
     * PUBLISH WORKSHEET
     * =====================================
     */
    @PostMapping("/{worksheetId}/publish")
    public void publishWorksheet(
            @PathVariable String worksheetId
    ) {
        publishService.publishWorksheet(worksheetId);
    }
}
