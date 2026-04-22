package com.tcon.learning_management_service.availability.repository;

import com.tcon.learning_management_service.availability.entity.TeacherAvailability;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface TeacherAvailabilityRepository extends MongoRepository<TeacherAvailability, String> {
    Optional<TeacherAvailability> findByTeacherId(String teacherId);
    void deleteByTeacherId(String teacherId);
}