package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.StudentQuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetSummaryResponse;
import com.tcon.learning_management_service.worksheet.entity.*;
import com.tcon.learning_management_service.worksheet.repository.*;
import com.tcon.learning_management_service.worksheet.mapper.WorksheetMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetStudentService {

    private final QuestionRepository questionRepository;
    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository worksheetVersionRepository;
    private final WorksheetAssignmentRepository assignmentRepository;
    private final WorksheetMapper worksheetMapper;
    private final WorksheetAttemptRepository attemptRepository;

    /*
     * =====================================
     * GET ASSIGNED WORKSHEETS
     * =====================================
     */
    public List<WorksheetSummaryResponse> getAssignedWorksheets(String studentId) {

        List<WorksheetAssignment> assignments =
                assignmentRepository.findByStudentId(studentId);

        if (assignments == null || assignments.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> worksheetIds = assignments.stream()
                .map(WorksheetAssignment::getWorksheetId)
                .toList();

        List<Worksheet> worksheets =
                worksheetRepository.findAllById(worksheetIds);

        return worksheets.stream()
                .filter(w -> w.getStatus() == WorksheetStatus.PUBLISHED)
                .map(worksheetMapper::toSummary)
                .toList();
    }

    /*
     * =====================================
     * 🔥 START WORKSHEET
     * =====================================
     */
    public void startWorksheet(String worksheetId, String studentId, String type) {

        log.info("Starting worksheet {} for student {} type {}", worksheetId, studentId, type);

        AttemptType attemptType = AttemptType.valueOf(type);

        // 🔒 Check existing active attempt
        Optional<WorksheetAttempt> existing =
                attemptRepository.findByStudentIdAndWorksheetIdAndAttemptTypeAndStatus(
                        studentId,
                        worksheetId,
                        attemptType,
                        AttemptStatus.IN_PROGRESS
                );

        if (existing.isPresent()) {
            throw new RuntimeException("Worksheet already in progress");
        }

        // ✅ ASSIGNED FLOW
        if (attemptType == AttemptType.ASSIGNED) {

            boolean assigned = assignmentRepository
                    .findByStudentId(studentId)
                    .stream()
                    .anyMatch(a -> a.getWorksheetId().equals(worksheetId));

            if (!assigned) {
                throw new RuntimeException("Student not assigned to this worksheet");
            }
        }

        // ✅ SELF PRACTICE FLOW
        if (attemptType == AttemptType.SELF_PRACTICE) {

            Worksheet worksheet = worksheetRepository.findById(worksheetId)
                    .orElseThrow(() -> new RuntimeException("Worksheet not found"));

            if (worksheet.getStatus() != WorksheetStatus.PUBLISHED) {
                throw new RuntimeException("Worksheet is not published");
            }
        }

        // ✅ CREATE ATTEMPT
        WorksheetAttempt attempt = WorksheetAttempt.builder()
                .worksheetId(worksheetId)
                .studentId(studentId)
                .attemptType(attemptType)
                .status(AttemptStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now())
                .build();

        attemptRepository.save(attempt);

        log.info("Worksheet attempt started successfully");
    }

    /*
     * =====================================
     * GET QUESTIONS
     * =====================================
     */
    public List<StudentQuestionResponse> getShuffledQuestions(String worksheetId, String studentId) {

        Worksheet worksheet = worksheetRepository.findById(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        if (worksheet.getStatus() != WorksheetStatus.PUBLISHED) {
            throw new RuntimeException("Worksheet is not published");
        }

        WorksheetVersion version = worksheetVersionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet version not found"));

        List<Question> questions = new ArrayList<>();

        for (var wq : version.getQuestions()) {
            Question q = questionRepository
                    .findByQuestionMasterIdAndQuestionVersionId(
                            wq.getQuestionMasterId(),
                            wq.getQuestionVersionId()
                    )
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            questions.add(q);
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found");
        }

        Collections.shuffle(questions);

        List<StudentQuestionResponse> responseList = new ArrayList<>();

        for (Question q : questions) {

            List<String> options = new ArrayList<>(q.getOptions());
            Collections.shuffle(options);

            StudentQuestionResponse res = new StudentQuestionResponse();
            res.setQuestionId(q.getId());
            res.setQuestionText(q.getQuestionText());
            res.setOptions(options);

            responseList.add(res);
        }

        return responseList;
    }

    /*
     * =====================================
     * SUBMIT WORKSHEET
     * =====================================
     */
    public WorksheetResultResponse submitWorksheet(SubmitWorksheetRequest request, String studentId) {

        log.info("Submitting worksheet {} for student {}", request.getWorksheetId(), studentId);

        // ✅ 1. BASIC VALIDATION
        if (request.getWorksheetId() == null) {
            throw new RuntimeException("WorksheetId required");
        }

        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new RuntimeException("Answers cannot be empty");
        }

        // ✅ 2. FETCH ACTIVE ATTEMPT (MANDATORY)
        WorksheetAttempt attempt = attemptRepository
                .findByStudentIdAndWorksheetIdAndStatus(
                        studentId,
                        request.getWorksheetId(),
                        AttemptStatus.IN_PROGRESS
                )
                .orElseThrow(() -> new RuntimeException("No active attempt found. Please start worksheet first."));

        // ✅ 3. FETCH LATEST VERSION
        WorksheetVersion version = worksheetVersionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(request.getWorksheetId())
                .orElseThrow(() -> new RuntimeException("Worksheet version not found"));

        // ✅ 4. LOAD QUESTIONS
        List<Question> questions = new ArrayList<>();

        for (var wq : version.getQuestions()) {
            Question q = questionRepository
                    .findByQuestionMasterIdAndQuestionVersionId(
                            wq.getQuestionMasterId(),
                            wq.getQuestionVersionId()
                    )
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            questions.add(q);
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found");
        }

        // ✅ 5. CREATE QUESTION MAP
        Map<String, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        // ✅ 6. EVALUATE
        int correct = 0;
        int wrong = 0;

        List<WorksheetResultResponse.QuestionResult> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : request.getAnswers().entrySet()) {

            Question q = questionMap.get(entry.getKey());

            if (q == null) {
                throw new RuntimeException("Invalid questionId: " + entry.getKey());
            }

            String correctAnswer = q.getOptions().get(q.getCorrectAnswerIndex());
            String studentAnswer = entry.getValue();

            boolean isCorrect =
                    studentAnswer != null &&
                            correctAnswer.equalsIgnoreCase(studentAnswer);

            if (isCorrect) correct++;
            else wrong++;

            results.add(
                    WorksheetResultResponse.QuestionResult.builder()
                            .questionId(q.getId())
                            .question(q.getQuestionText())
                            .correctAnswer(correctAnswer)
                            .studentAnswer(studentAnswer)
                            .reason(q.getReason())
                            .isCorrect(isCorrect)
                            .build()
            );
        }

        // ✅ 7. CALCULATE SCORE
        int total = questions.size();
        double percentage = total == 0 ? 0 : ((double) correct / total) * 100;

        // ✅ 8. UPDATE EXISTING ATTEMPT (IMPORTANT)
        attempt.setTotalQuestions(total);
        attempt.setCorrectAnswers(correct);
        attempt.setScore(percentage);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setAnswers(request.getAnswers());
        attempt.setStatus(AttemptStatus.SUBMITTED);

        attemptRepository.save(attempt);

        // ✅ 9. MARK ASSIGNMENT COMPLETED (ONLY IF ASSIGNED)
        if (attempt.getAttemptType() == AttemptType.ASSIGNED) {
            assignmentRepository
                    .findByStudentId(studentId)
                    .stream()
                    .filter(a -> a.getWorksheetId().equals(request.getWorksheetId()))
                    .findFirst()
                    .ifPresent(a -> {
                        a.setCompleted(true);
                        assignmentRepository.save(a);
                    });
        }

        // ✅ 10. RETURN RESPONSE
        return WorksheetResultResponse.builder()
                .totalQuestions(total)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .scorePercentage(percentage)
                .results(results)
                .build();
    }

    public List<WorksheetSummaryResponse> getPracticeWorksheets(
            String gradeId,
            String subjectId,
            String topicId
    ) {
        return worksheetRepository.findAll().stream()
                .filter(w -> w.getStatus() == WorksheetStatus.PUBLISHED)
                .filter(w -> w.getGradeId().equals(gradeId))
                .filter(w -> w.getSubjectId().equals(subjectId))
                .filter(w -> topicId == null || w.getTopicId().equals(topicId))
                .map(worksheetMapper::toSummary)
                .toList();
    }

    /*
     * =====================================
     * 🔥 GET RESULT (FINAL FIX)
     * =====================================
     */
    public WorksheetResultResponse getResult(String worksheetId, String studentId) {

        WorksheetAttempt attempt = attemptRepository
                .findByWorksheetIdAndStudentId(worksheetId, studentId)
                .orElseThrow(() -> new RuntimeException("Result not found"));

        WorksheetVersion version = worksheetVersionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(worksheetId)
                .orElseThrow(() -> new RuntimeException("Worksheet version not found"));

        List<Question> questions = new ArrayList<>();

        for (var wq : version.getQuestions()) {
            Question q = questionRepository
                    .findByQuestionMasterIdAndQuestionVersionId(
                            wq.getQuestionMasterId(),
                            wq.getQuestionVersionId()
                    )
                    .orElseThrow(() -> new RuntimeException("Question not found"));

            questions.add(q);
        }

        Map<String, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        List<WorksheetResultResponse.QuestionResult> results = new ArrayList<>();

        int correct = 0;
        int wrong = 0;

        for (Map.Entry<String, String> entry : attempt.getAnswers().entrySet()) {

            Question q = questionMap.get(entry.getKey());
            if (q == null) continue;

            String correctAnswer = q.getOptions().get(q.getCorrectAnswerIndex());
            String studentAnswer = entry.getValue();

            boolean isCorrect =
                    studentAnswer != null &&
                            correctAnswer.equalsIgnoreCase(studentAnswer);

            if (isCorrect) correct++;
            else wrong++;

            results.add(
                    WorksheetResultResponse.QuestionResult.builder()
                            .questionId(q.getId())
                            .question(q.getQuestionText())
                            .correctAnswer(correctAnswer)
                            .studentAnswer(studentAnswer)
                            .reason(q.getReason())
                            .isCorrect(isCorrect)
                            .build()
            );
        }

        int total = questions.size();
        double percentage = total == 0 ? 0 : ((double) correct / total) * 100;

        return WorksheetResultResponse.builder()
                .totalQuestions(total)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .scorePercentage(percentage)
                .results(results)
                .build();
    }
}