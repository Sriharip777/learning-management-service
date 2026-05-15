package com.tcon.learning_management_service.booking.repository;

import com.tcon.learning_management_service.booking.entity.Booking;
import com.tcon.learning_management_service.booking.entity.BookingStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {

    Optional<Booking> findBySessionIdAndStudentId(String sessionId, String studentId);

    List<Booking> findByStudentId(String studentId);

    boolean existsByStudentIdAndSessionStartTimeLessThanAndSessionEndTimeGreaterThanAndStatusIn(
            String studentId,
            Instant sessionStartTime,
            Instant sessionEndTime,
            List<BookingStatus> statuses
    );

    List<Booking> findByStudentIdAndStatus(String studentId, BookingStatus status);

    List<Booking> findByTeacherId(String teacherId);

    List<Booking> findByTeacherIdAndStatus(String teacherId, BookingStatus status);

    List<Booking> findBySessionId(String sessionId);

    List<Booking> findByParentId(String parentId);

    List<Booking> findBySessionIdAndStatus(String sessionId, BookingStatus status);

    List<Booking> findByCourseId(String courseId);

    List<Booking> findBySessionStartTimeBetween(Instant start, Instant end);

    List<Booking> findByStudentIdAndSessionStartTimeBetween(
            String studentId, Instant start, Instant end);

    List<Booking> findByTeacherIdAndSessionStartTimeBetween(
            String teacherId, Instant start, Instant end);

    List<Booking> findByTeacherIdAndSessionStartTimeLessThanAndSessionEndTimeGreaterThan(
            String teacherId,
            Instant slotEnd,
            Instant slotStart
    );

    Long countBySessionIdAndStatus(String sessionId, BookingStatus status);

    Long countByStudentIdAndStatus(String studentId, BookingStatus status);

    boolean existsBySessionIdAndStudentId(String sessionId, String studentId);

    boolean existsBySessionIdAndStudentIdAndStatus(String sessionId, String studentId, BookingStatus status);

    Optional<Booking> findByTeacherIdAndStudentIdAndSessionStartTimeAndSessionEndTime(
            String teacherId,
            String studentId,
            Instant sessionStartTime,
            Instant sessionEndTime
    );
}