// EnrollmentRepository.java
package com.tcon.learning_management_service.enrollment.repository;

import com.tcon.learning_management_service.enrollment.entity.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    List<Enrollment> findByStudentId(String studentId);

    @Query(value = "{ 'teacherId': ?0, 'status': 'ACTIVE' }", fields = "{ 'studentId': 1, '_id': 0 }")
    List<Enrollment> findActiveEnrollmentsByTeacherId(String teacherId);
}