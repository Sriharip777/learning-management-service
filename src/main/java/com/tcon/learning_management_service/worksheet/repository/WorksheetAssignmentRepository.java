package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.WorksheetAssignment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorksheetAssignmentRepository
        extends MongoRepository<WorksheetAssignment, String> {

    List<WorksheetAssignment> findByStudentId(String studentId);
}