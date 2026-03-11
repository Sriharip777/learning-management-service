package com.tcon.learning_management_service.course.repository;

import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    // Teacher-based queries
    List<Course> findByTeacherId(String teacherId);

    List<Course> findByTeacherIdAndStatus(String teacherId, CourseStatus status);

    // Status with pagination
    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    // Grade / subject / topic based
    List<Course> findByGradeId(String gradeId);

    List<Course> findBySubjectId(String subjectId);

    List<Course> findByTopicIdsIn(List<String> topicIds);

    // Grade level
    List<Course> findByGradeLevel(String gradeLevel);

    // Keyword search
    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    List<Course> searchByTitle(String keyword);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    List<Course> searchByKeyword(String keyword);

    // Price / rating
    List<Course> findByPricePerSessionBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<Course> findByRatingGreaterThanEqual(Double minRating);

    // Counts
    Long countByTeacherId(String teacherId);

    Long countByTeacherIdAndStatus(String teacherId, CourseStatus status);

    // Ownership check
    boolean existsByIdAndTeacherId(String id, String teacherId);
}
