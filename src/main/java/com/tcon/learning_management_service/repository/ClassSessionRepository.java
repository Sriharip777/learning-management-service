package com.tcon.learning_management_service.repository;


import com.tcon.learning_management_service.entity.ClassSession;
import com.tcon.learning_management_service.entity.ClassStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClassSessionRepository extends MongoRepository<ClassSession, String> {

    List<ClassSession> findByTeacherId(String teacherId);

    List<ClassSession> findByStudentId(String studentId);

    List<ClassSession> findByCourseId(String courseId);

    List<ClassSession> findByStatus(ClassStatus status);

    List<ClassSession> findByScheduledStartTimeBetween(LocalDateTime start, LocalDateTime end);

    List<ClassSession> findByReminderSentFalseAndScheduledStartTimeBefore(LocalDateTime time);
}
