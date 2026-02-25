package com.tcon.learning_management_service.assignment.repository;

import com.tcon.learning_management_service.assignment.entity.Assignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AssignmentRepository
        extends MongoRepository<Assignment, String> {

    List<Assignment> findByTeacherId(String teacherId);

    List<Assignment> findByStudentIdsContaining(String studentId);

}