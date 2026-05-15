package com.tcon.learning_management_service.session.repository;

import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.entity.SessionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClassSessionRepository extends MongoRepository<ClassSession, String> {

    List<ClassSession> findByCourseId(String courseId);

    List<ClassSession> findByCourseIdAndStatus(String courseId, ClassStatus status);

    List<ClassSession> findByTeacherId(String teacherId);

    List<ClassSession> findByTeacherIdAndStatus(String teacherId, ClassStatus status);

    @Query("{ 'participants.studentId': ?0 }")
    List<ClassSession> findByStudentId(String studentId);

    @Query("{ 'participants.studentId': ?0, 'status': ?1 }")
    List<ClassSession> findByStudentIdAndStatus(String studentId, ClassStatus status);

    List<ClassSession> findBySessionType(SessionType sessionType);

    Long countByCourseIdAndStatus(String courseId, ClassStatus status);

    Long countByTeacherIdAndStatus(String teacherId, ClassStatus status);

    boolean existsByIdAndTeacherId(String id, String teacherId);

    @Query("""
{
  "teacherId": ?0,
  "status": "SCHEDULED",
  "scheduledStartTime": { $lt: ?2 },
  "scheduledEndTime": { $gt: ?1 }
}
""")
    List<ClassSession> findOverlappingSessions(
            String teacherId, Instant start, Instant end);

    List<ClassSession> findByTeacherIdAndScheduledStartTimeBetween(
            String teacherId, Instant start, Instant end);

    List<ClassSession> findByScheduledStartTimeBetween(Instant start, Instant end);

    List<ClassSession> findByStatusAndScheduledStartTimeBefore(
            ClassStatus status, Instant dateTime);

    @Query("{ 'status': 'SCHEDULED', 'reminderSent': false, 'scheduledStartTime': { $gte: ?0, $lte: ?1 } }")
    List<ClassSession> findSessionsNeedingReminders(Instant start, Instant end);
}