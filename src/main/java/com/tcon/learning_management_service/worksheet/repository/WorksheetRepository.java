package com.tcon.learning_management_service.worksheet.repository;

import com.tcon.learning_management_service.worksheet.entity.Worksheet;
import com.tcon.learning_management_service.worksheet.entity.WorksheetStatus;
import com.tcon.learning_management_service.worksheet.entity.ReviewStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface WorksheetRepository extends MongoRepository<Worksheet, String> {

    // ✅ Get by status
    List<Worksheet> findByStatus(WorksheetStatus status);

    List<Worksheet> findByGradeId(String gradeId);

    // ✅ Basic filtering
    List<Worksheet> findBySubjectIdAndGradeId(
            String subjectId,
            String gradeId
    );

    // ✅ Filter with topic + status
    List<Worksheet> findByGradeIdAndSubjectIdAndTopicIdAndStatus(
            String gradeId,
            String subjectId,
            String topicId,
            WorksheetStatus status
    );

    // 🔥 REVIEW FLOW (YOUR FEATURE)
    List<Worksheet> findByStatusAndReviewStatus(
            WorksheetStatus status,
            ReviewStatus reviewStatus
    );

    // 🔥 LATEST WORKSHEETS (COLLEAGUE FEATURE)
    List<Worksheet> findByStatusOrderByCreatedAtDesc(
            WorksheetStatus status
    );
    List<Worksheet> findByGradeIdAndTopicIdAndStatus(
            String gradeId,
            String topicId,
            WorksheetStatus status
    );
}