package com.tcon.learning_management_service.session.repository;


import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.entity.SessionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClassSessionRepository extends MongoRepository<ClassSession, String> {

    List<ClassSession> findByCourseId(String courseId);

    List<ClassSession> findByCourseIdAndStatus(String courseId, ClassStatus status);

    List<ClassSession> findByTeacherId(String teacherId);

    List<ClassSession> findByTeacherIdAndStatus(String teacherId, ClassStatus status);

    List<ClassSession> findByTeacherIdAndScheduledStartTimeBetween(
            String teacherId, LocalDateTime start, LocalDateTime end);

    @Query("{ 'participants.studentId': ?0 }")
    List<ClassSession> findByStudentId(String studentId);

    @Query("{ 'participants.studentId': ?0, 'status': ?1 }")
    List<ClassSession> findByStudentIdAndStatus(String studentId, ClassStatus status);

    List<ClassSession> findByScheduledStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<ClassSession> findByStatusAndScheduledStartTimeBefore(ClassStatus status, LocalDateTime dateTime);

    List<ClassSession> findBySessionType(SessionType sessionType);

    @Query("{ 'status': 'SCHEDULED', 'reminderSent': false, 'scheduledStartTime': { $gte: ?0, $lte: ?1 } }")
    List<ClassSession> findSessionsNeedingReminders(LocalDateTime start, LocalDateTime end);

    Long countByCourseIdAndStatus(String courseId, ClassStatus status);

    Long countByTeacherIdAndStatus(String teacherId, ClassStatus status);

    boolean existsByIdAndTeacherId(String id, String teacherId);
}
