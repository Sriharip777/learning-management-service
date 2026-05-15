package com.tcon.learning_management_service.availability.repository;

import com.tcon.learning_management_service.availability.entity.DateSpecificAvailability;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DateSpecificAvailabilityRepository extends MongoRepository<DateSpecificAvailability, String> {

    List<DateSpecificAvailability> findByTeacherId(String teacherId);

    List<DateSpecificAvailability> findByTeacherIdAndDayStartUtcBetween(
            String teacherId, Instant from, Instant to);

    Optional<DateSpecificAvailability> findByTeacherIdAndDayStartUtc(
            String teacherId, Instant dayStartUtc);

    void deleteByTeacherIdAndDayStartUtc(String teacherId, Instant dayStartUtc);

    void deleteByTeacherId(String teacherId);
}