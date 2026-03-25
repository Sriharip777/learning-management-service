package com.tcon.learning_management_service.worksheet.service;

import com.tcon.learning_management_service.worksheet.dto.request.SubmitWorksheetRequest;
import com.tcon.learning_management_service.worksheet.dto.response.WorksheetResultResponse;
import com.tcon.learning_management_service.worksheet.entity.Question;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAttempt;
import com.tcon.learning_management_service.worksheet.repository.QuestionRepository;
import com.tcon.learning_management_service.worksheet.repository.WorksheetAttemptRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class WorksheetAttemptService {

    private final QuestionRepository questionRepository;
    private final WorksheetAttemptRepository attemptRepository;

    public WorksheetResultResponse submitWorksheet(SubmitWorksheetRequest request) {

        // ?? STEP 1: Check if already attempted
        Optional<WorksheetAttempt> existingAttempt =
                attemptRepository.findByWorksheetIdAndStudentId(
                        request.getWorksheetId(),
                        request.getStudentId()
                );

        // 📥 Fetch questions
        List<Question> questions =
                questionRepository.findByWorksheetId(request.getWorksheetId());

        // ✅ If already attempted → return previous result
        if (existingAttempt.isPresent()) {
            return buildResultFromAttempt(existingAttempt.get(), questions);
        }

        // 🧠 STEP 2: Evaluate new attempt
        int correctCount = 0;
        List<WorksheetResultResponse.QuestionResult> results = new ArrayList<>();

        for (Question q : questions) {

            int studentAnswer =
                    request.getAnswers().getOrDefault(q.getId(), -1);

            boolean isCorrect = studentAnswer == q.getCorrectAnswerIndex();

            if (isCorrect) correctCount++;

            results.add(
                    WorksheetResultResponse.QuestionResult.builder()
                            .questionId(Long.parseLong(q.getId()))
                            .question(q.getQuestionText())
                            .correctAnswer(q.getCorrectAnswerIndex())
                            .studentAnswer(studentAnswer)
                            .reason(q.getReason())
                            .isCorrect(isCorrect)
                            .build()
            );
        }

        int total = questions.size();
        int score = correctCount;

        // 💾 Save attempt
        WorksheetAttempt attempt = new WorksheetAttempt();
        attempt.setWorksheetId(request.getWorksheetId());
        attempt.setStudentId(request.getStudentId());
        attempt.setTotalQuestions(total);
        attempt.setCorrectAnswers(correctCount);
        attempt.setScore(score);
        attempt.setAnswers(request.getAnswers());
        attempt.setSubmittedAt(LocalDateTime.now());

        attemptRepository.save(attempt);

        // 📤 Return result
        return WorksheetResultResponse.builder()
                .totalQuestions(total)
                .correctAnswers(correctCount)
                .score(score)
                .results(results)
                .build();
    }

    /*
     * =====================================
     * HELPER METHOD: BUILD RESULT FROM OLD ATTEMPT
     * =====================================
     */
    private WorksheetResultResponse buildResultFromAttempt(
            WorksheetAttempt attempt,
            List<Question> questions
    ) {

        List<WorksheetResultResponse.QuestionResult> results = new ArrayList<>();

        for (Question q : questions) {

            int studentAnswer =
                    attempt.getAnswers().getOrDefault(q.getId(), -1);

            boolean isCorrect =
                    studentAnswer == q.getCorrectAnswerIndex();

            results.add(
                    WorksheetResultResponse.QuestionResult.builder()
                            .questionId(Long.parseLong(q.getId()))
                            .question(q.getQuestionText())
                            .correctAnswer(q.getCorrectAnswerIndex())
                            .studentAnswer(studentAnswer)
                            .reason(q.getReason())
                            .isCorrect(isCorrect)
                            .build()
            );
        }

        return WorksheetResultResponse.builder()
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .score(attempt.getScore())
                .results(results)
                .build();
    }
}