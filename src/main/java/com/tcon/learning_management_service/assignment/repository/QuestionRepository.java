package com.tcon.learning_management_service.assignment.repository;

import com.tcon.learning_management_service.assignment.entity.Question;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuestionRepository
        extends MongoRepository<Question, String> {

    List<Question> findByTeacherId(String teacherId);  // ✅ REQUIRED
}