package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.StudentQuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAttemptRepository;
import com.tcon.learning_management_service.worksheet.service.WorksheetService;
import com.tcon.learning_management_service.worksheet.service.WorksheetStudentService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/worksheets")
@RequiredArgsConstructor
public class WorksheetStudentController {

    // ✅ EXISTING
    private final WorksheetStudentService studentService;

    // ✅ NEW (ADDED)
    private final WorksheetService worksheetService;
    private final WorksheetAttemptRepository attemptRepository;

    /*
     * =====================================
     * 🔥 NEW: GET ASSIGNED WORKSHEETS
     * =====================================
     */
    @GetMapping("/assigned")
    public List<WorksheetSummaryResponse> getAssignedWorksheets(
            @RequestParam String studentId
    ) {
        return studentService.getAssignedWorksheets(studentId);
    }

    /*
     * =====================================
     * 🔥 START WORKSHEET (NEW)
     * =====================================
     */
    @PostMapping("/{worksheetId}/start")
    public String startWorksheet(
            @PathVariable String worksheetId,
            @RequestHeader("X-User-Id") String studentId
    ) {
        System.out.println("🔥 START WORKSHEET");
        System.out.println("Worksheet ID: " + worksheetId);
        System.out.println("Student ID: " + studentId);

        studentService.startWorksheet(worksheetId, studentId);

        return "Worksheet started successfully";
    }

    /*
     * =====================================
     * GET QUESTIONS (SHUFFLED)
     * =====================================
     */
    @GetMapping("/{worksheetId}/questions")
    public List<StudentQuestionResponse> getQuestions(
            @PathVariable String worksheetId,
            @RequestHeader("X-User-Id") String studentId
    ) {
        return studentService.getShuffledQuestions(worksheetId, studentId);
    }

    /*
     * =====================================
     * SUBMIT WORKSHEET (EVALUATE)
     * =====================================
     */
    @PostMapping("/submit")
    public WorksheetResultResponse submitWorksheet(
            @RequestBody SubmitWorksheetRequest request,
            @RequestHeader("X-User-Id") String studentId
    ) {
        return studentService.submitWorksheet(request, studentId);
    }

    /*
     * =====================================
     * 🔥 GET RESULT (NEW)
     * =====================================
     */
    @GetMapping("/{worksheetId}/result")
    public WorksheetResultResponse getResult(
            @PathVariable String worksheetId,
            @RequestHeader("X-User-Id") String studentId
    ) {
        return studentService.getResult(worksheetId, studentId);
    }

    /*
     * =====================================
     * 🔥 NEW: GET ALL RESULTS (ADDED)
     * =====================================
     */
    @GetMapping("/results")
    public List<WorksheetResultResponse> getAllResults(
            @RequestParam String studentId
    ) {
        return worksheetService.getAllResults(studentId);
    }
}