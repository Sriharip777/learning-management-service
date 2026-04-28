package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.AttemptStatus;
import com.tcon.learning_management_service.worksheet.entity.AttemptType;
import com.tcon.learning_management_service.worksheet.entity.WorksheetAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WorksheetAttemptRepository extends MongoRepository<WorksheetAttempt, String> {

    // ✅ Student attempts
    List<WorksheetAttempt> findByStudentId(String studentId);

    // ✅ One student + one worksheet (used for validation / single attempt)
    Optional<WorksheetAttempt> findByWorksheetIdAndStudentId(
            String worksheetId,
            String studentId
    );

    // 🔥 NEW: All attempts for a worksheet (teacher view)
    List<WorksheetAttempt> findByWorksheetId(String worksheetId);

    Optional<WorksheetAttempt> findByStudentIdAndWorksheetIdAndAttemptTypeAndStatus(
            String studentId,
            String worksheetId,
            AttemptType attemptType,
            AttemptStatus status
    );

    Optional<WorksheetAttempt> findByStudentIdAndWorksheetIdAndStatus(
            String studentId,
            String worksheetId,
            AttemptStatus status
    );
}