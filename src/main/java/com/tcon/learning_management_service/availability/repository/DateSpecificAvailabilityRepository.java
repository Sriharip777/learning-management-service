package com.tcon.learning_management_service.availability.repository;


import com.tcon.learning_management_service.availability.entity.DateSpecificAvailability;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DateSpecificAvailabilityRepository extends MongoRepository<DateSpecificAvailability, String> {

    List<DateSpecificAvailability> findByTeacherIdAndDateBetween(
            String teacherId, LocalDate startDate, LocalDate endDate);

    Optional<DateSpecificAvailability> findByTeacherIdAndDate(String teacherId, LocalDate date);

    void deleteByTeacherIdAndDate(String teacherId, LocalDate date);

    void deleteByTeacherId(String teacherId);

    List<DateSpecificAvailability> findByTeacherId(String teacherId);
}