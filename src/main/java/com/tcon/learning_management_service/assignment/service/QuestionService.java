package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.QuestionCreateRequest;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.repository.QuestionRepository;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    /**
     * Teacher creates question (independent of assignment)
     */
    public Question createQuestion(QuestionCreateRequest request) {

        Question question = new Question();

        question.setQuestionText(request.getQuestionText());
        question.setOptions(request.getOptions());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setTeacherId(request.getTeacherId());

        return questionRepository.save(question);
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
}