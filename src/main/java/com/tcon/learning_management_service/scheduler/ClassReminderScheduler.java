package com.tcon.learning_management_service.scheduler;

import com.tcon.learning_management_service.entity.ClassSession;
import com.tcon.learning_management_service.event.SessionEventPublisher;
import com.tcon.learning_management_service.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassReminderScheduler {

    private final ClassSessionRepository sessionRepository;
    private final SessionEventPublisher sessionEventPublisher;

    @Value("${app.scheduler.reminder-hours-before:2}")
    private int reminderHoursBefore;

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void sendClassReminders() {
        log.debug("Checking for sessions that need reminders");

        LocalDateTime reminderTime = LocalDateTime.now().plusHours(reminderHoursBefore);

        List<ClassSession> sessionsNeedingReminders =
                sessionRepository.findByReminderSentFalseAndScheduledStartTimeBefore(reminderTime);

        for (ClassSession session : sessionsNeedingReminders) {
            log.info("Sending reminder for session: {}", session.getId());

            // Publish reminder event
            sessionEventPublisher.publishSessionReminder(session);

            // Mark reminder as sent
            session.setReminderSent(true);
            sessionRepository.save(session);
        }
    }
}
