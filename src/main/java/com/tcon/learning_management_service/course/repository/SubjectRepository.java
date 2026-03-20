package com.tcon.learning_management_service.course.repository;

import com.tcon.learning_management_service.course.entity.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends MongoRepository<Subject, String> {
    List<Subject> findByGradeIdAndIsActiveTrue(String gradeId);
    List<Subject> findByGradeId(String gradeId);

    Optional<Subject> findByGradeIdAndNameIgnoreCase(String gradeId, String name);

}