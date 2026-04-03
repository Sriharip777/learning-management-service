package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.StudentQuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.service.WorksheetStudentService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/worksheets")
@RequiredArgsConstructor
public class WorksheetStudentController {

    private final WorksheetStudentService studentService;

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
     * GET QUESTIONS (SHUFFLED)
     * =====================================
     */
    @GetMapping("/{worksheetId}/questions")
    public List<StudentQuestionResponse> getQuestions(
            @PathVariable String worksheetId
    ) {
        return studentService.getShuffledQuestions(worksheetId);
    }

    /*
     * =====================================
     * SUBMIT WORKSHEET (EVALUATE)
     * =====================================
     */
    @PostMapping("/submit")
    public WorksheetResultResponse submitWorksheet(
            @RequestBody SubmitWorksheetRequest request
    ) {
        return studentService.submitWorksheet(request);
    }
}