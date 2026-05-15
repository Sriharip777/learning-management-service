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
    List<Course> findByTeacherIdsContaining(String teacherId);

    List<Course> findByTeacherId(String teacherId);

    List<Course> findByTeacherIdAndStatus(String teacherId, CourseStatus status);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    List<Course> findByGradeId(String gradeId);

    List<Course> findBySubjectId(String subjectId);

    List<Course> findByTopicIdsIn(List<String> topicIds);

    List<Course> findByGradeLevel(String gradeLevel);

    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    List<Course> searchByTitle(String keyword);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    List<Course> searchByKeyword(String keyword);

    List<Course> findByPricePerSessionBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<Course> findByRatingGreaterThanEqual(Double minRating);

    Long countByTeacherId(String teacherId);

    Long countByTeacherIdAndStatus(String teacherId, CourseStatus status);

    boolean existsByIdAndTeacherId(String id, String teacherId);
}