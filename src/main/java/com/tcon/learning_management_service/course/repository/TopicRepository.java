package com.tcon.learning_management_service.course.repository;

import com.tcon.learning_management_service.course.entity.Topic;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TopicRepository extends MongoRepository<Topic, String> {
    List<Topic> findBySubjectIdAndIsActiveTrue(String subjectId);
    List<Topic> findBySubjectId(String subjectId);
}
