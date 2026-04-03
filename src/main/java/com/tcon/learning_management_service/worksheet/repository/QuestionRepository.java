package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.Question;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends MongoRepository<Question, String> {

    // ✅ Find specific version of question
    Optional<Question> findByQuestionMasterIdAndQuestionVersionId(
            String questionMasterId,
            String questionVersionId
    );

    // 🔥 NEW: Get all questions for a worksheet
    List<Question> findByWorksheetId(String worksheetId);
}