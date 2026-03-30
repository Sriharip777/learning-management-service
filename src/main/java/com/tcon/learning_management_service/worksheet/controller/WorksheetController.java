package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.QuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.UploadResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.service.WorksheetAttemptService;
import com.tcon.learning_management_service.worksheet.service.WorksheetQueryService;
import com.tcon.learning_management_service.worksheet.service.WorksheetService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/worksheets")
@RequiredArgsConstructor
public class WorksheetController {

    private final WorksheetQueryService queryService;
    private final WorksheetService worksheetService;
    private final WorksheetAttemptService worksheetAttemptService;

    /*
     * =====================================
     * GET WORKSHEET DETAILS
     * =====================================
     */
    @GetMapping("/{worksheetId}")
    public WorksheetDetailResponse getWorksheet(
            @PathVariable String worksheetId
    ) {
        return queryService.getWorksheetDetails(worksheetId);
    }

    /*
     * =====================================
     * GET AVAILABLE WORKSHEETS (FILTERED)
     * =====================================
     */
    @GetMapping("/available")
    public List<WorksheetDetailResponse> getAvailableWorksheets(
            @RequestParam String gradeId,
            @RequestParam String subjectId,
            @RequestParam String topicId
    ) {
        return queryService.getPublishedWorksheetDetails(
                gradeId,
                subjectId,
                topicId
        );
    }

    /*
     * =====================================
     * UPLOAD QUESTIONS (EXCEL) 🔥 (FIXED)
     * =====================================
     */
    @PostMapping("/{worksheetId}/questions/upload")
    public ResponseEntity<UploadResponse> uploadQuestions(
            @PathVariable String worksheetId,
            @RequestParam("file") MultipartFile file
    ) {

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new RuntimeException("Only Excel (.xlsx) files are allowed");
        }

        UploadResponse response = worksheetService.uploadQuestionsFromExcel(
                worksheetId,
                file
        );

        return ResponseEntity.ok(response);
    }

    /*
     * =====================================
     * GET QUESTIONS FOR STUDENT
     * =====================================
     */
    @GetMapping("/{worksheetId}/questions")
    public ResponseEntity<List<QuestionResponse>> getQuestions(
            @PathVariable String worksheetId
    ) {
        return ResponseEntity.ok(
                worksheetAttemptService.getShuffledQuestions(worksheetId)
        );
    }

    /*
     * =====================================
     * SUBMIT WORKSHEET (STUDENT)
     * =====================================
     */
    @PostMapping("/submit")
    public ResponseEntity<WorksheetResultResponse> submitWorksheet(
            @RequestBody SubmitWorksheetRequest request
    ) {
        return ResponseEntity.ok(
                worksheetAttemptService.submitWorksheet(request)
        );
    }
}