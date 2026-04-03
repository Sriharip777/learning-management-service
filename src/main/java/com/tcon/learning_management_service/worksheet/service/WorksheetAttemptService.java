package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.QuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.entity.Question;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAttempt;
import com.tcon.learning_management_service.worksheet.repository.QuestionRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAttemptRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetAttemptService {

    private final QuestionRepository questionRepository;
    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository worksheetVersionRepository;
    private final WorksheetAttemptRepository worksheetAttemptRepository;

    /*
     * =====================================
     * GET QUESTIONS (SHUFFLED)
     * =====================================
     */
    public List<QuestionResponse> getShuffledQuestions(String worksheetId) {

        log.info("Fetching shuffled questions for worksheet={}", worksheetId);

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
                    .findById(wq.getQuestionMasterId())
                    .orElseThrow(() -> new RuntimeException(
                            "Question not found: " + wq.getQuestionMasterId()
                    ));

            questions.add(q);
        }

        if (questions.isEmpty()) {
            throw new RuntimeException("No questions found");
        }

        Collections.shuffle(questions);

        List<QuestionResponse> responseList = new ArrayList<>();

        for (Question q : questions) {

            List<String> options = new ArrayList<>(q.getOptions());
            Collections.shuffle(options);

            QuestionResponse res = QuestionResponse.builder()
                    .id(q.getId())
                    .questionText(q.getQuestionText())
                    .options(options)
                    .build();

            responseList.add(res);
        }

        return responseList;
    }

    /*
     * =====================================
     * SUBMIT WORKSHEET (EVALUATE + SAVE)
     * =====================================
     */
    public WorksheetResultResponse submitWorksheet(SubmitWorksheetRequest request) {

        log.info("Submitting worksheet for student={}", request.getStudentId());

        // 🔥 PREVENT MULTIPLE ATTEMPTS
        Optional<WorksheetAttempt> existingAttempt =
                worksheetAttemptRepository.findByWorksheetIdAndStudentId(
                        request.getWorksheetId(),
                        request.getStudentId()
                );

        if (existingAttempt.isPresent()) {
            throw new RuntimeException("You have already submitted this worksheet");
        }

        Worksheet worksheet = worksheetRepository.findById(request.getWorksheetId())
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        if (worksheet.getStatus() != WorksheetStatus.PUBLISHED) {
            throw new RuntimeException("Worksheet is not published");
        }

        WorksheetVersion version = worksheetVersionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(request.getWorksheetId())
                .orElseThrow(() -> new RuntimeException("Worksheet version not found"));

        Map<String, Question> questionMap = new HashMap<>();

        for (var wq : version.getQuestions()) {
            Question q = questionRepository
                    .findById(wq.getQuestionMasterId())
                    .orElseThrow(() -> new RuntimeException(
                            "Question not found: " + wq.getQuestionMasterId()
                    ));

            questionMap.put(q.getId(), q);
        }

        if (questionMap.isEmpty()) {
            throw new RuntimeException("No questions found");
        }

        int correct = 0;
        int wrong = 0;

        List<WorksheetResultResponse.QuestionResult> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : request.getAnswers().entrySet()) {

            String questionId = entry.getKey();
            String studentAnswer = entry.getValue();

            Question q = questionMap.get(questionId);
            if (q == null) continue;

            String correctAnswer = null;

            if (q.getOptions() != null &&
                    q.getCorrectAnswerIndex() != null &&
                    q.getCorrectAnswerIndex() >= 0 &&
                    q.getCorrectAnswerIndex() < q.getOptions().size()) {

                correctAnswer = q.getOptions().get(q.getCorrectAnswerIndex());
            }

            if (correctAnswer == null) {
                log.error("Invalid question data: {}", q);
                throw new RuntimeException("Invalid question data for questionId=" + q.getId());
            }

            boolean isCorrect = correctAnswer.equals(studentAnswer);

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

        int total = questionMap.size();
        double percentage = total == 0 ? 0 : ((double) correct / total) * 100;

        log.info("Evaluation done: correct={}, wrong={}", correct, wrong);

        // 🔥 SAVE ATTEMPT (COLLEAGUE CHANGE)
        WorksheetAttempt attempt = new WorksheetAttempt();
        attempt.setWorksheetId(request.getWorksheetId());
        attempt.setStudentId(request.getStudentId());
        attempt.setTotalQuestions(total);
        attempt.setCorrectAnswers(correct);
        attempt.setScore(percentage);
        attempt.setSubmittedAt(LocalDateTime.now());
        attempt.setAnswers(request.getAnswers());

        worksheetAttemptRepository.save(attempt);

        return WorksheetResultResponse.builder()
                .totalQuestions(total)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .scorePercentage(percentage)
                .results(results)
                .build();
    }
}