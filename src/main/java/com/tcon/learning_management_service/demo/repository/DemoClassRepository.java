package com.tcon.learning_management_service.demo.repository;

import com.tcon.learning_management_service.demo.entity.DemoClass;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DemoClassRepository extends MongoRepository<DemoClass, String> {

    List<DemoClass> findByStudentId(String studentId);

    List<DemoClass> findByStudentIdAndStatus(String studentId, DemoClass.DemoStatus status);

    List<DemoClass> findByTeacherId(String teacherId);

    List<DemoClass> findByTeacherIdAndStatus(String teacherId, DemoClass.DemoStatus status);

    List<DemoClass> findByCourseId(String courseId);

    List<DemoClass> findByScheduledStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<DemoClass> findByStudentIdAndTeacherId(String studentId, String teacherId);

    Long countByStudentIdAndStatus(String studentId, DemoClass.DemoStatus status);

    Long countByStudentId(String studentId);

    boolean existsByStudentIdAndCourseIdAndStatus(String studentId, String courseId, DemoClass.DemoStatus status);
}
