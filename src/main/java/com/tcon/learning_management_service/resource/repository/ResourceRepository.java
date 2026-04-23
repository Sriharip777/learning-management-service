package com.tcon.learning_management_service.resource.repository;

import com.tcon.learning_management_service.resource.entity.Resource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends MongoRepository<Resource, String> {
    List<Resource> findByIsActiveTrueOrderByUploadedAtDesc();
    List<Resource> findByTopicIdAndIsActiveTrueOrderByUploadedAtDesc(String topicId);
    List<Resource> findByTopicIdInAndIsActiveTrueOrderByUploadedAtDesc(List<String> topicIds);
    List<Resource> findByGradeIdAndIsActiveTrueOrderByUploadedAtDesc(String gradeId);
    List<Resource> findByGradeIdAndSubjectIdAndIsActiveTrueOrderByUploadedAtDesc(String gradeId, String subjectId);
    List<Resource> findByGradeIdAndSubjectIdAndTopicIdAndIsActiveTrueOrderByUploadedAtDesc(String gradeId, String subjectId, String topicId);
    List<Resource> findByIsActiveOrderByUploadedAtDesc(Boolean isActive);
}