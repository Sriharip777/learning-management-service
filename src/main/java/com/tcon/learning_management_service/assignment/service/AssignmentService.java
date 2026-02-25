package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.AssignmentCreateRequest;
import com.tcon.learning_management_service.assignment.dto.AssignStudentsRequest;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import com.tcon.learning_management_service.assignment.validation.AssignmentValidator;
import com.tcon.learning_management_service.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentValidator assignmentValidator;


    /**
     * Teacher creates assignment
     */
    public Assignment createAssignment(
            AssignmentCreateRequest request)
    {

        assignmentValidator.validateCreateRequest(request);

        Assignment assignment = new Assignment();

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setTeacherId(request.getTeacherId());
        assignment.setDueDate(request.getDueDate());

        assignment.setQuestionIds(new ArrayList<>());
        assignment.setStudentIds(new ArrayList<>());

        assignment.setStatus("CREATED");

        return assignmentRepository.save(assignment);
    }


    /**
     * Assign students
     */
    public Assignment assignStudents(
            String assignmentId,
            AssignStudentsRequest request)
    {

        Assignment assignment =
                assignmentRepository.findById(assignmentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Assignment not found: " + assignmentId));

        assignment.setStudentIds(request.getStudentIds());

        assignment.setStatus("ACTIVE");

        return assignmentRepository.save(assignment);
    }


    /**
     * Student assignments
     */
    public List<Assignment> getAssignmentsForStudent(
            String studentId)
    {

        return assignmentRepository
                .findByStudentIdsContaining(studentId);
    }


    /**
     * Teacher assignments
     */
    public List<Assignment> getAssignmentsForTeacher(
            String teacherId)
    {

        return assignmentRepository
                .findByTeacherId(teacherId);
    }


    /**
     * Get assignment
     */
    public Assignment getAssignment(String assignmentId)
    {

        return assignmentRepository.findById(assignmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Assignment not found: " + assignmentId));
    }

}