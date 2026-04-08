package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.QuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.UploadResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAttempt;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAttemptRepository;
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

    // 🔥 ADDED (from colleague)
    private final WorksheetAttemptRepository worksheetAttemptRepository;

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
     * UPLOAD QUESTIONS (EXCEL)
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

    /*
     * =====================================
     * 🔥 NEW: GET RESULTS FOR TEACHER
     * =====================================
     */
    @GetMapping("/{worksheetId}/results")
    public ResponseEntity<List<WorksheetAttempt>> getResults(
            @PathVariable String worksheetId
    ) {
        List<WorksheetAttempt> attempts =
                worksheetAttemptRepository.findByWorksheetId(worksheetId);

        return ResponseEntity.ok(attempts);
    }

    /*
     * =====================================
     * 🔥 UPDATED: PREVIEW WORKSHEET (FIXED)
     * =====================================
     */
    @GetMapping("/{worksheetId}/preview")
    public WorksheetDetailResponse previewWorksheet(
            @PathVariable String worksheetId
    ) {
        try {
            return queryService.getWorksheetDetails(worksheetId);
        } catch (Exception e) {
            throw new RuntimeException("Preview failed: " + e.getMessage());
        }
    }

    /*
     * =====================================
     * 🔥 NEW: TEACHER ASSIGNMENT HISTORY
     * =====================================
     */
    @GetMapping("/assigned/history")
    public ResponseEntity<List<WorksheetSummaryResponse>> getTeacherAssignmentHistory(
            @RequestHeader("X-User-Id") String teacherId
    ) {
        return ResponseEntity.ok(
                worksheetService.getTeacherAssignmentHistory(teacherId)
        );
    }
    /*
     * =====================================
     * 🔥 NEW: UPDATE QUESTIONS (ADMIN EDIT)
     * =====================================
     */
    @PostMapping("/{worksheetId}/update-questions")
    public ResponseEntity<Void> updateQuestions(
            @PathVariable String worksheetId,
            @RequestBody List<com.tcon.learning_management_service.worksheet.dto.request.QuestionUpdateRequest> questions
    ) {
        worksheetService.updateQuestions(worksheetId, questions);
        return ResponseEntity.ok().build();
    }
}