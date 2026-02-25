package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.QuestionCreateRequest;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import com.tcon.learning_management_service.assignment.repository.QuestionRepository;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final AssignmentRepository assignmentRepository;


    /**
     * Teacher adds question to assignment
     * Auto wiring Assignment ↔ Question
     */
    public Question addQuestion(QuestionCreateRequest request)
    {

        Assignment assignment =
                assignmentRepository.findById(
                                request.getAssignmentId())
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assignment not found: "
                                                + request.getAssignmentId()));

        Question question = new Question();

        question.setAssignmentId(request.getAssignmentId());
        question.setQuestionText(request.getQuestionText());
        question.setCorrectAnswer(request.getCorrectAnswer());
        question.setMarks(request.getMarks());

        Question savedQuestion =
                questionRepository.save(question);


        // Auto wiring
        assignment.getQuestionIds()
                .add(savedQuestion.getId());

        assignmentRepository.save(assignment);

        return savedQuestion;

    }


    /**
     * Get questions by assignment
     */
    public List<Question> getQuestions(String assignmentId)
    {

        // Validate assignment
        assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + assignmentId));

        return questionRepository
                .findByAssignmentId(assignmentId);
    }


    /**
     * Get single question
     */
    public Question getQuestion(String questionId)
    {

        return questionRepository.findById(questionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Question not found: " + questionId));
    }

}