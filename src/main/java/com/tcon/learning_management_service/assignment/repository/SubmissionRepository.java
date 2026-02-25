package com.tcon.learning_management_service.assignment.repository;

import com.tcon.learning_management_service.assignment.entity.Submission;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionRepository extends MongoRepository<Submission, String> {

    Optional<Submission> findByAssignmentIdAndStudentId(String assignmentId, String studentId);

    List<Submission> findByAssignmentId(String assignmentId);
}