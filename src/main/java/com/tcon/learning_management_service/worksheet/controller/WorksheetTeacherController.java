package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.AssignWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.QuestionFlag;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAttempt;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAttemptRepository;
import com.tcon.learning_management_service.worksheet.integration.AssignmentIntegrationService;
import com.tcon.learning_management_service.worksheet.service.WorksheetQueryService;
import com.tcon.learning_management_service.worksheet.service.WorksheetTeacherService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teacher/worksheets")
@RequiredArgsConstructor
public class WorksheetTeacherController {

    private final WorksheetQueryService queryService;
    private final AssignmentIntegrationService assignmentIntegrationService;
    private final WorksheetTeacherService teacherService;
    private final WorksheetAttemptRepository worksheetAttemptRepository;

    /*
     * =====================================
     * GET AVAILABLE WORKSHEETS
     * =====================================
     */
    @GetMapping
    public List<WorksheetSummaryResponse> getWorksheets(
            @RequestParam String subjectId,
            @RequestParam String gradeId,
            @RequestParam(required = false) String topicId
    ) {
        return queryService.getPublishedWorksheets(subjectId, gradeId, topicId);
    }

    /*
     * =====================================
     * 🔥 GET LATEST WORKSHEETS
     * =====================================
     */
    @GetMapping("/latest")
    public List<WorksheetSummaryResponse> getLatestWorksheets() {
        return queryService.getLatestPublishedWorksheets();
    }

    /*
     * =====================================
     * GET PENDING REVIEW WORKSHEETS
     * =====================================
     */
    @GetMapping("/pending-review")
    public List<WorksheetSummaryResponse> getPendingReviewWorksheets(
            @RequestParam String subjectId,
            @RequestParam String gradeId,
            @RequestParam(required = false) String topicId
    ) {
        return teacherService.getPendingReviewWorksheets(subjectId, gradeId, topicId);
    }

    /*
     * =====================================
     * 🔥 APPROVE WORKSHEET
     * =====================================
     */
    @PostMapping("/{worksheetId}/approve")
    public String approveWorksheet(
            @PathVariable String worksheetId,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        if (teacherId == null || teacherId.isBlank()) {
            throw new RuntimeException("Teacher ID is required");
        }

        teacherService.approveWorksheet(worksheetId, teacherId);
        return "Worksheet approved successfully";
    }

    /*
     * =====================================
     * 🔥 FLAG QUESTIONS
     * =====================================
     */
    @PostMapping("/{worksheetId}/flag")
    public String flagQuestions(
            @PathVariable String worksheetId,
            @RequestParam Integer version,
            @RequestBody List<QuestionFlag> flags,
            @RequestHeader("X-User-Id") String teacherId
    ) {
        if (teacherId == null || teacherId.isBlank()) {
            throw new RuntimeException("Teacher ID is required");
        }

        if (version == null) {
            throw new RuntimeException("Version is required");
        }

        if (flags == null || flags.isEmpty()) {
            throw new RuntimeException("At least one question must be flagged");
        }

        teacherService.flagQuestions(worksheetId, version, teacherId, flags);
        return "Questions flagged successfully";
    }

    /*
     * =====================================
     * 🔥 GET RESULTS (NEW)
     * =====================================
     */
    @GetMapping("/{worksheetId}/results")
    public List<WorksheetAttempt> getResults(
            @PathVariable String worksheetId
    ) {
        return worksheetAttemptRepository.findByWorksheetId(worksheetId);
    }

    /*
     * =====================================
     * ASSIGN WORKSHEET (MERGED LOGIC 🔥)
     * =====================================
     */
    @PostMapping("/{worksheetId}/assign")
    public String assignWorksheet(
            @PathVariable String worksheetId,
            @RequestBody AssignWorksheetRequest request
    ) {

        // ✅ Step 1: Validate worksheet state
        teacherService.validateWorksheetBeforeAssign(worksheetId);

        // ✅ Step 2: Ensure path/body consistency
        if (request.getWorksheetId() == null) {
            request.setWorksheetId(worksheetId);
        }

        if (!worksheetId.equals(request.getWorksheetId())) {
            throw new RuntimeException("WorksheetId mismatch between path and body");
        }

        // ✅ Step 3: Save assignments (DB)
        teacherService.assignWorksheet(request);

        // ✅ Step 4: (Optional) External integration
        assignmentIntegrationService.assignWorksheet(
                worksheetId,
                request.getTeacherId(),
                request.getStudentIds(),
                request.getDueDate()
        );

        return "Worksheet assigned successfully";
    }
}