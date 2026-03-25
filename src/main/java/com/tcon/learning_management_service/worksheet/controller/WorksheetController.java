package com.tcon.learning_management_service.worksheet.controller;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.QuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.entity.Question;
import com.tcon.learning_management_service.worksheet.service.WorksheetAttemptService;
import com.tcon.learning_management_service.worksheet.service.WorksheetQueryService;
import com.tcon.learning_management_service.worksheet.service.WorksheetService;
import com.tcon.learning_management_service.worksheet.repository.QuestionRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/worksheets")
@RequiredArgsConstructor
public class WorksheetController {

    private final WorksheetQueryService queryService;
    private final WorksheetService worksheetService;
    private final WorksheetAttemptService worksheetAttemptService;
    private final QuestionRepository questionRepository;

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
    public ResponseEntity<String> uploadQuestions(
            @PathVariable String worksheetId,
            @RequestParam("file") MultipartFile file
    ) {

        String result = worksheetService.uploadQuestionsFromExcel(
                worksheetId,
                file
        );

        return ResponseEntity.ok(result);
    }

    /*
     * =====================================
     * GET QUESTIONS FOR STUDENT (SHUFFLED + SAFE)
     * =====================================
     */
    @GetMapping("/{worksheetId}/questions")
    public List<QuestionResponse> getQuestions(
            @PathVariable String worksheetId
    ) {

        List<Question> questions =
                questionRepository.findByWorksheetId(worksheetId);

        // 🔀 Shuffle questions
        Collections.shuffle(questions);

        return questions.stream()
                .map(this::mapToShuffledResponse)
                .toList();
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
     * HELPER METHOD: SHUFFLE OPTIONS
     * =====================================
     */
    private QuestionResponse mapToShuffledResponse(Question q) {

        List<String> options = new ArrayList<>(q.getOptions());

        // 🔀 Shuffle options
        Collections.shuffle(options);

        return QuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .options(options)
                .build();
    }
}