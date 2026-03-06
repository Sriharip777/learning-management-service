package com.tcon.learning_management_service.assignment.repository;

import com.tcon.learning_management_service.assignment.model.AssignmentAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AssignmentAttemptRepository
        extends MongoRepository<AssignmentAttempt, String> {

    Optional<AssignmentAttempt> findByAssignmentIdAndStudentId(
            String assignmentId, String studentId);
}