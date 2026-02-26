package com.tcon.learning_management_service.assignment.validation;

import com.tcon.learning_management_service.assignment.dto.AssignmentCreateRequest;
import com.tcon.learning_management_service.assignment.dto.AssignStudentsRequest;
import com.tcon.learning_management_service.assignment.entity.Question;
import com.tcon.learning_management_service.assignment.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AssignmentValidator {

    private final QuestionRepository questionRepository;

    /**
     * Validate assignment creation request
     */
    public void validateCreateRequest(
            AssignmentCreateRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Assignment title is required");
        }

        if (request.getTeacherId() == null || request.getTeacherId().trim().isEmpty()) {
            throw new IllegalArgumentException("TeacherId is required");
        }

        if (request.getQuestionIds() == null || request.getQuestionIds().isEmpty()) {
            throw new IllegalArgumentException("Assignment must contain at least one question");
        }

        // Validate questionIds exist in DB
        List<Question> questions =
                questionRepository.findAllById(request.getQuestionIds());

        if (questions.size() != request.getQuestionIds().size()) {
            throw new IllegalArgumentException("One or more questionIds are invalid");
        }
    }


    /**
     * Validate student assignment request
     */
    public void validateStudentAssignment(
            AssignStudentsRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Assign request cannot be null");
        }

        if (request.getStudentIds() == null || request.getStudentIds().isEmpty()) {
            throw new IllegalArgumentException("At least one student must be assigned");
        }
    }
}