package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.WorksheetAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorksheetAttemptRepository extends MongoRepository<WorksheetAttempt, String> {

    List<WorksheetAttempt> findByStudentId(String studentId);

    // ✅ THIS IS REQUIRED (your error is because of this missing)
    Optional<WorksheetAttempt> findByWorksheetIdAndStudentId(String worksheetId, String studentId);
}