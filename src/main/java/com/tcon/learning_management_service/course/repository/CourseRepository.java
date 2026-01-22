package com.tcon.learning_management_service.course.repository;

import com.tcon.learning_management_service.course.entity.Course;
import com.tcon.learning_management_service.course.entity.CourseCategory;
import com.tcon.learning_management_service.course.entity.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends MongoRepository<Course, String> {

    List<Course> findByTeacherId(String teacherId);

    List<Course> findByTeacherIdAndStatus(String teacherId, CourseStatus status);

    Page<Course> findByStatus(CourseStatus status, Pageable pageable);

    Page<Course> findByCategory(CourseCategory category, Pageable pageable);

    Page<Course> findByCategoryAndStatus(CourseCategory category, CourseStatus status, Pageable pageable);

    List<Course> findByGradeLevel(String gradeLevel);

    List<Course> findByTagsContaining(String tag);

    @Query("{ 'title': { $regex: ?0, $options: 'i' } }")
    List<Course> searchByTitle(String keyword);

    @Query("{ $or: [ { 'title': { $regex: ?0, $options: 'i' } }, { 'description': { $regex: ?0, $options: 'i' } } ] }")
    List<Course> searchByKeyword(String keyword);

    List<Course> findByPricePerSessionBetween(BigDecimal minPrice, BigDecimal maxPrice);

    List<Course> findByRatingGreaterThanEqual(Double minRating);

    @Query("{ 'teacherId': ?0, 'status': { $in: ['PUBLISHED', 'ACTIVE'] } }")
    List<Course> findActiveTeacherCourses(String teacherId);

    Long countByTeacherId(String teacherId);

    Long countByTeacherIdAndStatus(String teacherId, CourseStatus status);

    boolean existsByIdAndTeacherId(String id, String teacherId);
}
