package com.tcon.learning_management_service.repository;


import com.tcon.learning_management_service.entity.Course;
import com.tcon.learning_management_service.entity.CourseCategory;
import com.tcon.learning_management_service.entity.CourseStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByTeacherId(String teacherId);

    List<Course> findByStatus(CourseStatus status);

    List<Course> findByCategory(CourseCategory category);

    List<Course> findByTeacherIdAndStatus(String teacherId, CourseStatus status);

    List<Course> findByCategoryAndStatus(CourseCategory category, CourseStatus status);

    List<Course> findByIsRecurring(Boolean isRecurring);
}
