package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.response.ErrorRow;
import com.tcon.learning_management_service.assignment.dto.response.UploadResponse;
import com.tcon.learning_management_service.assignment.dto.QuestionCreateRequest;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.repository.AssignmentQuestionRepository;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final AssignmentQuestionRepository questionRepository;

    /**
     * ✅ NEW: Teacher creates question (manual)
     */
    public Question createQuestion(QuestionCreateRequest request) {

        Question question = new Question();

        question.setQuestionText(request.getQuestionText());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setReason(request.getReason());
        question.setTeacherId(request.getTeacherId());

        return questionRepository.save(question);
    }

    /**
     * Teacher creates questions via Excel
     */
    public UploadResponse uploadQuestionsFromExcel(String teacherId, MultipartFile file) {

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

                    Question question = new Question();
                    question.setQuestionText(q);
                    question.setOptions(List.of(a, b, c, d));
                    question.setCorrectAnswer(correct.toUpperCase());
                    question.setReason(reason);
                    question.setTeacherId(teacherId);

                    questions.add(question);

                } catch (Exception ex) {
                    errors.add(new ErrorRow(i + 1, ex.getMessage()));
                }
            }

            if (questions.isEmpty()) {
                throw new RuntimeException("No valid questions found");
            }

            questionRepository.saveAll(questions);

        } catch (Exception e) {
            throw new RuntimeException("Excel processing failed: " + e.getMessage());
        }

        return new UploadResponse(questions.size(), errors.size(), errors);
    }

    /**
     * Get single question
     */
    public Question getQuestion(String questionId) {

        return questionRepository.findById(questionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Question not found: " + questionId));
    }

    /**
     * Get all questions created by teacher
     */
    public List<Question> getQuestionsByTeacher(String teacherId) {
        return questionRepository.findByTeacherId(teacherId);
    }

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
}