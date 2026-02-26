package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.AssignmentCreateRequest;
import com.tcon.learning_management_service.assignment.dto.AssignStudentsRequest;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.entity.AssignmentStatus; // ✅ IMPORT ENUM
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import com.tcon.learning_management_service.assignment.validation.AssignmentValidator;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentValidator assignmentValidator;

    /**
     * Teacher creates assignment
     * Assignment must contain questionIds
     */
    public Assignment createAssignment(AssignmentCreateRequest request) {

        // 1️⃣ Validate request
        assignmentValidator.validateCreateRequest(request);

        Assignment assignment = new Assignment();

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setTeacherId(request.getTeacherId());
        assignment.setDueDate(request.getDueDate());

        // Must contain questions
        assignment.setQuestionIds(request.getQuestionIds());

        // studentIds already initialized in entity (if you followed previous update)
        assignment.setStatus(AssignmentStatus.CREATED);   // ✅ ENUM

        return assignmentRepository.save(assignment);
    }

    /**
     * Assign students to assignment
     */
    public Assignment assignStudents(String assignmentId,
                                     AssignStudentsRequest request) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + assignmentId));

        assignmentValidator.validateStudentAssignment(request);

        assignment.setStudentIds(request.getStudentIds());
        assignment.setStatus(AssignmentStatus.ACTIVE);   // ✅ ENUM

        return assignmentRepository.save(assignment);
    }

    /**
     * Get assignments for student
     */
    public List<Assignment> getAssignmentsForStudent(String studentId) {

        return assignmentRepository
                .findByStudentIdsContaining(studentId);
    }

    /**
     * Get assignments for teacher
     */
    public List<Assignment> getAssignmentsForTeacher(String teacherId) {

        return assignmentRepository
                .findByTeacherId(teacherId);
    }

    /**
     * Get single assignment
     */
    public Assignment getAssignment(String assignmentId) {

        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + assignmentId));
    }
}