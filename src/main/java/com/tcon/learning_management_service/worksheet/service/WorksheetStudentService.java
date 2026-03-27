package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.StudentQuestionResponse;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.entity.Question;
import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.WorksheetVersion;
import com.tcon.learning_management_service.worksheet.repository.QuestionRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetVersionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorksheetStudentService {

    private final QuestionRepository questionRepository;
    private final WorksheetRepository worksheetRepository;
    private final WorksheetVersionRepository worksheetVersionRepository;

    /*
     * =====================================
     * GET QUESTIONS (SHUFFLED)
     * =====================================
     */
    public List<StudentQuestionResponse> getShuffledQuestions(String worksheetId) {

        log.info("Fetching questions for worksheet={}", worksheetId);

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
     * SUBMIT WORKSHEET (EVALUATE)
     * =====================================
     */
    public WorksheetResultResponse submitWorksheet(SubmitWorksheetRequest request) {

        log.info("Submitting worksheet for student={}", request.getStudentId());

        Worksheet worksheet = worksheetRepository.findById(request.getWorksheetId())
                .orElseThrow(() -> new RuntimeException("Worksheet not found"));

        if (worksheet.getStatus() != WorksheetStatus.PUBLISHED) {
            throw new RuntimeException("Worksheet is not published");
        }

        WorksheetVersion version = worksheetVersionRepository
                .findTopByWorksheetIdOrderByVersionNumberDesc(request.getWorksheetId())
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

        Map<String, Question> questionMap = new HashMap<>();
        for (Question q : questions) {
            questionMap.put(q.getId(), q);
        }

        int correct = 0;
        int wrong = 0;

        List<WorksheetResultResponse.QuestionResult> results = new ArrayList<>();

        for (Map.Entry<String, String> entry : request.getAnswers().entrySet()) {

            String questionId = entry.getKey();
            String studentAnswer = entry.getValue();

            Question question = questionMap.get(questionId);
            if (question == null) continue;

            // ✅ SAFE CORRECT ANSWER EXTRACTION (FIXED)
            String correctAnswer = null;

            if (question.getOptions() != null &&
                    question.getCorrectAnswerIndex() != null &&
                    question.getCorrectAnswerIndex() >= 0 &&
                    question.getCorrectAnswerIndex() < question.getOptions().size()) {

                correctAnswer = question.getOptions()
                        .get(question.getCorrectAnswerIndex());
            }

            if (correctAnswer == null) {
                log.error("❌ Invalid question data: {}", question);
                throw new RuntimeException("Invalid question data for questionId=" + question.getId());
            }

            boolean isCorrect = correctAnswer.equals(studentAnswer);

            if (isCorrect) correct++;
            else wrong++;

            results.add(
                    WorksheetResultResponse.QuestionResult.builder()
                            .questionId(question.getId())
                            .question(question.getQuestionText())
                            .correctAnswer(correctAnswer)
                            .studentAnswer(studentAnswer)
                            .reason(question.getReason())
                            .isCorrect(isCorrect)
                            .build()
            );
        }

        int total = questions.size();
        double percentage = total == 0 ? 0 : ((double) correct / total) * 100;

        log.info("Evaluation completed: correct={}, wrong={}", correct, wrong);

        return WorksheetResultResponse.builder()
                .totalQuestions(total)
                .correctAnswers(correct)
                .wrongAnswers(wrong)
                .scorePercentage(percentage)
                .results(results)
                .build();
    }
}