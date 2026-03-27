package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.Question;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface QuestionRepository extends MongoRepository<Question, String> {

    // ✅ ONLY THIS
    Optional<Question> findByQuestionMasterIdAndQuestionVersionId(
            String questionMasterId,
            String questionVersionId
    );
}