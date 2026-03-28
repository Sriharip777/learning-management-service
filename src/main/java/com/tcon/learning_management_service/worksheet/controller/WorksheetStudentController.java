package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.StudentQuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.service.WorksheetStudentService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/student/worksheets")
@RequiredArgsConstructor
public class WorksheetStudentController {

    private final WorksheetStudentService studentService;

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