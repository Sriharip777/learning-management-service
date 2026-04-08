package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.CreateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.request.QuestionUpdateRequest;
import com.tcon.learning_management_service.worksheet.dto.request.UpdateWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.ErrorRow;
import com.tcon.learning_management_service.worksheet.dto.response.UploadResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetDetailResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.*;
import com.tcon.learning_management_service.worksheet.integration.CourseIntegrationService;
import com.tcon.learning_management_service.worksheet.integration.dto.TopicDto;
import com.tcon.learning_management_service.worksheet.mapper.WorksheetMapper;
import com.tcon.learning_management_service.worksheet.repository.*;
import com.tcon.learning_management_service.worksheet.validation.WorksheetValidator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetService {

    private final WorksheetRepository worksheetRepository;
    private final QuestionRepository questionRepository;
    private final WorksheetVersionRepository worksheetVersionRepository;
    private final WorksheetMapper worksheetMapper;
    private final WorksheetValidator worksheetValidator;
    private final CourseIntegrationService courseIntegrationService;

    // ✅ ADDED (for teacher assignment history)
    private final WorksheetAssignmentRepository assignmentRepository;

    /*
     * =====================================
     * CREATE WORKSHEET
     * =====================================
     */
    public WorksheetResponse createWorksheet(CreateWorksheetRequest request, String createdBy) {

        if (request.getGradeId() == null || request.getSubjectId() == null || request.getTopicId() == null) {
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
        worksheet.setHasQuestions(false);
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

        if (request.getTitle() != null) {
            worksheet.setTitle(request.getTitle());
        }

        if (request.getEstimatedDuration() != null) {
            worksheet.setDuration(request.getEstimatedDuration());
        }

        worksheet.setUpdatedAt(LocalDateTime.now());

        Worksheet updated = worksheetRepository.save(worksheet);
        return worksheetMapper.toResponse(updated);
    }

    /*
     * =====================================
     * UPLOAD QUESTIONS FROM EXCEL
     * =====================================
     */
    public UploadResponse uploadQuestionsFromExcel(String worksheetId, MultipartFile file) {

        log.info("Uploading questions for worksheet={}", worksheetId);

        Worksheet worksheet = worksheetRepository
                .findById(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        worksheetValidator.validateEditable(worksheet);

        if (file.isEmpty() || file.getOriginalFilename() == null ||
                !file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
            throw new RuntimeException("Upload valid .xlsx file");
        }

        List<Question> questions = new ArrayList<>();
        List<ErrorRow> errors = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

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
                    question.setWorksheetId(worksheetId);
                    question.setQuestionMasterId(UUID.randomUUID().toString());
                    question.setQuestionVersionId(UUID.randomUUID().toString());
                    question.setQuestionText(q);
                    question.setOptions(options);
                    question.setCorrectAnswerIndex(correctIndex);
                    question.setReason(reason);

                    questions.add(question);

                } catch (Exception ex) {
                    errors.add(new ErrorRow(i + 1, ex.getMessage()));
                }
            }

            if (questions.isEmpty()) {
                throw new RuntimeException("No valid questions found");
            }

            List<Question> savedQuestions = questionRepository.saveAll(questions);

            int nextVersion = worksheetVersionRepository
                    .findTopByWorksheetIdOrderByVersionNumberDesc(worksheetId)
                    .map(v -> v.getVersionNumber() + 1)
                    .orElse(1);

            WorksheetVersion version = new WorksheetVersion();
            version.setWorksheetId(worksheetId);
            version.setVersionNumber(nextVersion);
            version.setStatus(WorksheetStatus.DRAFT);
            version.setCreatedAt(LocalDateTime.now());

            List<WorksheetQuestionRef> versionQuestions = new ArrayList<>();
            int order = 1;

            for (Question q : savedQuestions) {

                WorksheetQuestionRef ref = new WorksheetQuestionRef();
                ref.setQuestionMasterId(q.getQuestionMasterId());
                ref.setQuestionVersionId(q.getQuestionVersionId());
                ref.setOrderIndex(order++);
                ref.setMarks(5);

                versionQuestions.add(ref);
            }

            version.setQuestions(versionQuestions);
            version.setQuestionCount(versionQuestions.size());

            worksheetVersionRepository.save(version);

            worksheet.setHasQuestions(true);
            worksheet.setCurrentVersion(nextVersion);
            worksheet.setUpdatedAt(LocalDateTime.now());

            worksheetRepository.save(worksheet);

        } catch (Exception e) {
            log.error("Excel processing failed", e);
            throw new RuntimeException("Excel processing failed: " + e.getMessage());
        }

        return new UploadResponse(questions.size(), errors.size(), errors);
    }

    /*
     * =====================================
     * GET REJECTED WORKSHEETS
     * =====================================
     */
    public List<WorksheetSummaryResponse> getRejectedWorksheets() {

        List<Worksheet> worksheets = worksheetRepository
                .findByStatusAndReviewStatus(
                        WorksheetStatus.PUBLISHED,
                        ReviewStatus.REJECTED
                );

        return worksheets.stream()
                .map(worksheetMapper::toSummary)
                .toList();
    }

    /*
     * =====================================
     * 🔥 NEW: GET ALL WORKSHEETS
     * =====================================
     */
    public List<Worksheet> getAllWorksheets() {
        return worksheetRepository.findAll();
    }

    /*
     * =====================================
     * 🔥 NEW: TEACHER ASSIGNMENT HISTORY
     * =====================================
     */
    public List<WorksheetSummaryResponse> getTeacherAssignmentHistory(String teacherId) {

        List<WorksheetAssignment> assignments =
                assignmentRepository.findByTeacherId(teacherId);

        Map<String, List<WorksheetAssignment>> grouped =
                assignments.stream()
                        .collect(Collectors.groupingBy(WorksheetAssignment::getWorksheetId));

        List<String> worksheetIds = new ArrayList<>(grouped.keySet());

        List<Worksheet> worksheets =
                worksheetRepository.findAllById(worksheetIds);

        return worksheets.stream().map(ws -> {

            WorksheetSummaryResponse response = worksheetMapper.toSummary(ws);

            List<WorksheetAssignment> wsAssignments = grouped.get(ws.getId());

            response.setAssignedCount(wsAssignments.size());

            response.setLastAssignedAt(
                    wsAssignments.stream()
                            .map(WorksheetAssignment::getAssignedAt)
                            .max(LocalDateTime::compareTo)
                            .orElse(null)
            );

            long completedCount = wsAssignments.stream()
                    .filter(WorksheetAssignment::isCompleted)
                    .count();

            response.setCompletedCount((int) completedCount);
            response.setPendingCount(wsAssignments.size() - (int) completedCount);

            return response;

        }).toList();
    }

    public void updateQuestions(String worksheetId, List<QuestionUpdateRequest> updatedQuestions) {

        for (QuestionUpdateRequest req : updatedQuestions) {

            Question q = questionRepository
                    .findByQuestionMasterIdAndQuestionVersionId(
                            req.getQuestionMasterId(),
                            req.getQuestionVersionId()
                    )
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            q.setQuestionText(req.getQuestionText());
            q.setOptions(req.getOptions());
            q.setReason(req.getReason());

            questionRepository.save(q);
        }
    }

    /*
     * =====================================
     * PREVIEW WORKSHEET (ADD THIS METHOD)
     * =====================================
     */
    public WorksheetDetailResponse getWorksheetPreview(String worksheetId) {

        // 1️⃣ Fetch worksheet
        Worksheet worksheet = worksheetRepository
                .findById(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        // 2️⃣ Fetch latest version
        WorksheetVersion version = worksheetVersionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(worksheetId)
                .orElseThrow(() -> new RuntimeException("No version found"));

        if (version.getQuestions() == null || version.getQuestions().isEmpty()) {
            throw new RuntimeException("No questions found in version");
        }

        // 3️⃣ Fetch actual questions using version refs
        List<WorksheetDetailResponse.QuestionResponse> questionResponses =
                version.getQuestions().stream().map(ref -> {

                            Question q = questionRepository
                                    .findByQuestionMasterIdAndQuestionVersionId(
                                            ref.getQuestionMasterId(),
                                            ref.getQuestionVersionId()
                                    )
                                    .orElseThrow(() -> new RuntimeException("Question not found"));

                            WorksheetDetailResponse.QuestionResponse qr =
                                    new WorksheetDetailResponse.QuestionResponse();

                            qr.setQuestionMasterId(q.getQuestionMasterId());
                            qr.setQuestionVersionId(q.getQuestionVersionId());
                            qr.setQuestionText(q.getQuestionText());
                            qr.setOptions(q.getOptions());
                            qr.setReason(q.getReason());

                            // ✅ IMPORTANT: use version data
                            qr.setOrderIndex(ref.getOrderIndex());
                            qr.setMarks(ref.getMarks());

                            return qr;

                        }).sorted(Comparator.comparing(WorksheetDetailResponse.QuestionResponse::getOrderIndex))
                        .toList();

        // 4️⃣ Build response
        WorksheetDetailResponse response = new WorksheetDetailResponse();

        response.setWorksheetId(worksheet.getId());
        response.setTitle(worksheet.getTitle());
        response.setVersion(version.getVersionNumber());
        response.setQuestions(questionResponses);

        return response;
    }
    /*
     * =====================================
     * HELPERS
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

    private void validateRow(int row, String q, String a, String b, String c, String d, String correct) {

        if (q == null || q.isBlank())
            throw new RuntimeException("Row " + row + ": Question is empty");

        if (a == null || b == null || c == null || d == null)
            throw new RuntimeException("Row " + row + ": Options missing");

        if (correct == null || !"ABCD".contains(correct.toUpperCase()))
            throw new RuntimeException("Row " + row + ": Correct must be A/B/C/D");
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