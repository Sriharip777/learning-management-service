// EnrollmentRepository.java
package com.tcon.learning_management_service.enrollment.repository;

import com.tcon.learning_management_service.enrollment.entity.Enrollment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface EnrollmentRepository extends MongoRepository<Enrollment, String> {

    List<Enrollment> findByStudentId(String studentId);
}