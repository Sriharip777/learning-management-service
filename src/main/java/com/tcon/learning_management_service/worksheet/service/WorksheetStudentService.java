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
    public void startWorksheet(String worksheetId, String studentId) {

        log.info("Starting worksheet {} for student {}", worksheetId, studentId);

        boolean assigned = assignmentRepository
                .findByStudentId(studentId)
                .stream()
                .anyMatch(a -> a.getWorksheetId().equals(worksheetId));

        if (!assigned) {
            throw new RuntimeException("Student not assigned to this worksheet");
        }

        Optional<WorksheetAttempt> existing =
                attemptRepository.findByWorksheetIdAndStudentId(worksheetId, studentId);

        if (existing.isPresent()) {
            log.info("Worksheet already started/attempted");
            return;
        }

        log.info("Worksheet start validated");
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

        log.info("Submitting worksheet: {}", request);

        if (request.getWorksheetId() == null) {
            throw new RuntimeException("WorksheetId required");
        }

        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            throw new RuntimeException("Answers cannot be empty");
        }

        Optional<WorksheetAttempt> existing =
                attemptRepository.findByWorksheetIdAndStudentId(
                        request.getWorksheetId(),
                        studentId
                );

        if (existing.isPresent()) {
            throw new RuntimeException("Worksheet already attempted");
        }

        WorksheetVersion version = worksheetVersionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(request.getWorksheetId())
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

        int correct = 0;
        int wrong = 0;

        List<WorksheetResultResponse.QuestionResult> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : request.getAnswers().entrySet()) {

            Question q = questionMap.get(entry.getKey());

            if (q == null) {
                throw new RuntimeException("Invalid questionId");
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

        int total = questions.size();
        double percentage = (double) correct / total * 100;

        WorksheetAttempt attempt = new WorksheetAttempt();
        attempt.setWorksheetId(request.getWorksheetId());
        attempt.setStudentId(studentId);
        attempt.setTotalQuestions(total);
        attempt.setCorrectAnswers(correct);
        attempt.setScore(percentage);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setAnswers(request.getAnswers());

        attemptRepository.save(attempt);

        // 🔥 Mark assignment completed
        assignmentRepository
                .findByStudentId(request.getStudentId())
                .stream()
                .filter(a -> a.getWorksheetId().equals(request.getWorksheetId()))
                .findFirst()
                .ifPresent(a -> {
                    a.setCompleted(true);
                    assignmentRepository.save(a);
                });

        return WorksheetResultResponse.builder()
                .totalQuestions(total)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .scorePercentage(percentage)
                .results(results)
                .build();
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