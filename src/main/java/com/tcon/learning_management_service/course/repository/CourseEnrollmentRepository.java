package com.tcon.learning_management_service.course.repository;

import com.tcon.learning_management_service.course.entity.CourseEnrollment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseEnrollmentRepository extends MongoRepository<CourseEnrollment, String> {

    Optional<CourseEnrollment> findByCourseIdAndStudentId(String courseId, String studentId);

    List<CourseEnrollment> findByStudentId(String studentId);

    List<CourseEnrollment> findByStudentIdAndStatus(String studentId, CourseEnrollment.EnrollmentStatus status);

    List<CourseEnrollment> findByCourseId(String courseId);

    List<CourseEnrollment> findByCourseIdAndStatus(String courseId, CourseEnrollment.EnrollmentStatus status);

    Long countByCourseIdAndStatus(String courseId, CourseEnrollment.EnrollmentStatus status);

    boolean existsByCourseIdAndStudentId(String courseId, String studentId);

    boolean existsByCourseIdAndStudentIdAndStatus(String courseId, String studentId, CourseEnrollment.EnrollmentStatus status);
}
