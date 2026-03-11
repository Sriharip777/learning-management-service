package com.tcon.learning_management_service.course.repository;

import com.tcon.learning_management_service.course.entity.Grade;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface GradeRepository extends MongoRepository<Grade, String> {
    List<Grade> findByIsActiveTrueOrderByOrderAsc();
    boolean existsByName(String name);
}
