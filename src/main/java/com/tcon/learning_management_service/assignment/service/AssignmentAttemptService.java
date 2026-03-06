package com.tcon.learning_management_service.assignment.service;

import com.tcon.learning_management_service.assignment.dto.StartAssignmentRequest;
import com.tcon.learning_management_service.assignment.entity.Assignment;
import com.tcon.learning_management_service.assignment.model.AssignmentAttempt;
import com.tcon.learning_management_service.assignment.repository.AssignmentAttemptRepository;
import com.tcon.learning_management_service.assignment.repository.AssignmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssignmentAttemptService {

    private final AssignmentAttemptRepository repository;
    private final AssignmentRepository assignmentRepository;

    // 1️⃣ Start Assignment
    public AssignmentAttempt startAssignment(String assignmentId, StartAssignmentRequest request) {

        return repository.findByAssignmentIdAndStudentId(
                        assignmentId, request.getStudentId())
                .orElseGet(() -> {

                    AssignmentAttempt attempt = new AssignmentAttempt();

                    attempt.setAssignmentId(assignmentId);
                    attempt.setStudentId(request.getStudentId());

                    LocalDateTime now = LocalDateTime.now();

                    // Start time
                    attempt.setAttendedAt(now);

                    // First activity time
                    attempt.setLastActiveAt(now);

                    return repository.save(attempt);
                });
    }

    // 2️⃣ Update Student Activity
    public AssignmentAttempt updateActivity(String assignmentId, String studentId) {

        AssignmentAttempt attempt = repository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new RuntimeException("Assignment attempt not found"));

        attempt.setLastActiveAt(LocalDateTime.now());

        return repository.save(attempt);
    }

    // 3️⃣ Submit Assignment
    public AssignmentAttempt submitAssignment(String assignmentId, String studentId) {

        AssignmentAttempt attempt = repository
                .findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new RuntimeException("Assignment attempt not found"));

        // Prevent multiple submissions
        if (attempt.getSubmittedAt() != null) {
            throw new RuntimeException("Assignment already submitted");
        }

        LocalDateTime submittedTime = LocalDateTime.now();

        attempt.setSubmittedAt(submittedTime);

        // Update last activity
        attempt.setLastActiveAt(submittedTime);

        // Calculate time spent
        long timeSpent = Duration
                .between(attempt.getAttendedAt(), submittedTime)
                .toSeconds();

        attempt.setTimeSpentSeconds(timeSpent);

        // 🔹 Detect late submission
        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        if (assignment.getDueDate() != null && submittedTime.isAfter(assignment.getDueDate())) {
            attempt.setLateSubmission(true);
        } else {
            attempt.setLateSubmission(false);
        }

        return repository.save(attempt);
    }
}