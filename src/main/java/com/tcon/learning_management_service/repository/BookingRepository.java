package com.tcon.learning_management_service.repository;


import com.tcon.learning_management_service.entity.Booking;
import com.tcon.learning_management_service.entity.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    List<Booking> findByStudentId(String studentId);

    List<Booking> findByTeacherId(String teacherId);

    List<Booking> findByCourseId(String courseId);

    List<Booking> findByStatus(BookingStatus status);

    List<Booking> findByTeacherIdAndStartTimeBetween(String teacherId, LocalDateTime start, LocalDateTime end);

    List<Booking> findByStudentIdAndStatus(String studentId, BookingStatus status);
}
