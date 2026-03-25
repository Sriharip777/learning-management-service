package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorksheetRepository extends MongoRepository<Worksheet, String> {

    List<Worksheet> findByStatus(WorksheetStatus status);

    List<Worksheet> findBySubjectIdAndGradeId(
            String subjectId,
            String gradeId
    );

    List<Worksheet> findByGradeIdAndSubjectIdAndTopicIdAndStatus(
            String gradeId,
            String subjectId,
            String topicId,
            WorksheetStatus status
    );
}