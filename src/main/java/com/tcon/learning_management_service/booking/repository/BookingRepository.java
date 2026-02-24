package com.tcon.learning_management_service.booking.repository;

import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    Optional<Booking> findBySessionIdAndStudentId(String sessionId, String studentId);

    List<Booking> findByStudentId(String studentId);

    List<Booking> findByStudentIdAndStatus(String studentId, BookingStatus status);

    List<Booking> findByTeacherId(String teacherId);

    List<Booking> findByTeacherIdAndStatus(String teacherId, BookingStatus status);

    List<Booking> findBySessionId(String sessionId);

    // ✅ NEW: all bookings for a parent (all their children)
    List<Booking> findByParentId(String parentId);

    List<Booking> findBySessionIdAndStatus(String sessionId, BookingStatus status);

    List<Booking> findByCourseId(String courseId);

    List<Booking> findBySessionStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<Booking> findByStudentIdAndSessionStartTimeBetween(
            String studentId, LocalDateTime start, LocalDateTime end);

    List<Booking> findByTeacherIdAndSessionStartTimeBetween(
            String teacherId, LocalDateTime start, LocalDateTime end);

    Long countBySessionIdAndStatus(String sessionId, BookingStatus status);

    Long countByStudentIdAndStatus(String studentId, BookingStatus status);

    boolean existsBySessionIdAndStudentId(String sessionId, String studentId);

    boolean existsBySessionIdAndStudentIdAndStatus(String sessionId, String studentId, BookingStatus status);
}