package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.CreateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.request.UpdateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResponse;
import com.tcon.learning_management_service.worksheet.entity.Question;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.integration.CourseIntegrationService;
import com.tcon.learning_management_service.worksheet.integration.dto.TopicDto;
import com.tcon.learning_management_service.worksheet.mapper.WorksheetMapper;
import com.tcon.learning_management_service.worksheet.repository.QuestionRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetService {

    private final WorksheetRepository worksheetRepository;
    private final QuestionRepository questionRepository;
    private final WorksheetMapper worksheetMapper;
    private final WorksheetValidator worksheetValidator;
    private final CourseIntegrationService courseIntegrationService;

    /*
     * =====================================
     * CREATE WORKSHEET
     * =====================================
     */
    public WorksheetResponse createWorksheet(
            CreateWorksheetRequest request,
            String createdBy
    ) {

        if (request.getGradeId() == null ||
                request.getSubjectId() == null ||
                request.getTopicId() == null) {
            throw new RuntimeException("Grade, Subject, Topic are required");
        }

        TopicDto topic = courseIntegrationService.validateSubjectAndGrade(
                request.getSubjectId(),
                request.getGradeId(),
                request.getTopicId()
        );

        Worksheet worksheet = worksheetMapper.toEntity(request, createdBy);

        worksheet.setGradeId(request.getGradeId());
        worksheet.setSubjectId(request.getSubjectId());
        worksheet.setTopicId(request.getTopicId());

        worksheet.setDuration(topic.getDuration());
        worksheet.setStatus(WorksheetStatus.DRAFT);
        worksheet.setCreatedAt(LocalDateTime.now());

        Worksheet saved = worksheetRepository.save(worksheet);

        return worksheetMapper.toResponse(saved);
    }

    /*
     * =====================================
     * UPDATE WORKSHEET
     * =====================================
     */
    public WorksheetResponse updateWorksheet(UpdateWorksheetRequest request) {

        Worksheet worksheet = worksheetRepository
                .findById(request.getWorksheetId())
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        worksheetValidator.validateEditable(worksheet);

        worksheet.setTitle(request.getTitle());
        worksheet.setUpdatedAt(LocalDateTime.now());

        Worksheet updated = worksheetRepository.save(worksheet);

        return worksheetMapper.toResponse(updated);
    }

    /*
     * =====================================
     * GET WORKSHEET
     * =====================================
     */
    public WorksheetResponse getWorksheet(String worksheetId) {

        Worksheet worksheet = worksheetRepository
                .findById(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        return worksheetMapper.toResponse(worksheet);
    }

    /*
     * =====================================
     * GET PUBLISHED WORKSHEETS
     * =====================================
     */
    public List<WorksheetResponse> getPublishedWorksheets(
            String gradeId,
            String subjectId,
            String topicId
    ) {

        List<Worksheet> worksheets =
                worksheetRepository
                        .findByGradeIdAndSubjectIdAndTopicIdAndStatus(
                                gradeId,
                                subjectId,
                                topicId,
                                WorksheetStatus.PUBLISHED
                        );

        return worksheets.stream()
                .map(worksheetMapper::toResponse)
                .toList();
    }

    /*
     * =====================================
     * UPLOAD QUESTIONS FROM EXCEL
     * =====================================
     */
    public String uploadQuestionsFromExcel(
            String worksheetId,
            MultipartFile file
    ) {

        log.info("Uploading questions for worksheet={}", worksheetId);

        Worksheet worksheet = worksheetRepository
                .findById(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        worksheetValidator.validateEditable(worksheet);

        if (file.isEmpty() ||
                file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new RuntimeException("Upload valid .xlsx file");
        }

        List<Question> questions = new ArrayList<>();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getLastRowNum() == 0) {
                throw new RuntimeException("Excel file contains no data");
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {

                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {

                    String q = getCell(row, 0);
                    String a = getCell(row, 1);
                    String b = getCell(row, 2);
                    String c = getCell(row, 3);
                    String d = getCell(row, 4);
                    String correct = getCell(row, 5);
                    String reason = getCell(row, 6);

                    validateRow(i, q, a, b, c, d, correct);

                    List<String> options = List.of(a, b, c, d);
                    int correctIndex = mapCorrect(correct);

                    Question question = new Question();

                    // ✅ FIXED PART (IMPORTANT)
                    question.setQuestionMasterId(UUID.randomUUID().toString());
                    question.setQuestionVersionId("v1");

                    question.setQuestionText(q);
                    question.setOptions(options);
                    question.setCorrectAnswerIndex(correctIndex);
                    question.setReason(reason);

                    questions.add(question);
                    success++;

                } catch (Exception ex) {
                    failed++;
                    errors.add("Row " + i + ": " + ex.getMessage());
                }
            }

            if (questions.isEmpty()) {
                throw new RuntimeException("No valid questions found in Excel");
            }

            questionRepository.saveAll(questions);

        } catch (Exception e) {
            log.error("Excel processing failed", e);
            throw new RuntimeException("Excel processing failed: " + e.getMessage());
        }

        log.info("Excel processed: success={}, failed={}", success, failed);

        return "Uploaded: " + success + ", Failed: " + failed + ", Errors: " + errors;
    }

    /*
     * =====================================
     * HELPER METHODS
     * =====================================
     */

    private String getCell(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            default -> null;
        };
    }

    private void validateRow(int row,
                             String q,
                             String a,
                             String b,
                             String c,
                             String d,
                             String correct) {

        if (q == null || q.isBlank())
            throw new RuntimeException("Question is empty");

        if (a == null || b == null || c == null || d == null)
            throw new RuntimeException("Options missing");

        if (correct == null || !"ABCD".contains(correct.toUpperCase()))
            throw new RuntimeException("Correct must be A/B/C/D");
    }

    private int mapCorrect(String correct) {
        return switch (correct.toUpperCase()) {
            case "A" -> 0;
            case "B" -> 1;
            case "C" -> 2;
            case "D" -> 3;
            default -> throw new RuntimeException("Invalid correct answer");
        };
    }
}