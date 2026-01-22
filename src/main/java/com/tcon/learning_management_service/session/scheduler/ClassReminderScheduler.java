package com.tcon.learning_management_service.session.scheduler;

import com.tcon.learning_management_service.event.SessionEventPublisher;
import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClassReminderScheduler {

    private final ClassSessionRepository sessionRepository;
    private final SessionEventPublisher eventPublisher;

    /**
     * Runs every 10 minutes to send reminders for upcoming sessions
     */
    @Scheduled(cron = "0 */10 * * * *")
    @Transactional
    public void sendSessionReminders() {
        log.info("Checking for sessions needing reminders");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderWindow = now.plusHours(1); // Send reminder 1 hour before

        // Find sessions that need reminders
        List<ClassSession> sessionsNeedingReminders = sessionRepository
                .findSessionsNeedingReminders(now, reminderWindow);

        int remindersSent = 0;
        for (ClassSession session : sessionsNeedingReminders) {
            try {
                // Publish reminder event
                eventPublisher.publishSessionReminder(session);

                // Mark reminder as sent
                session.setReminderSent(true);
                session.setReminderSentAt(LocalDateTime.now());
                sessionRepository.save(session);

                remindersSent++;
            } catch (Exception e) {
                log.error("Failed to send reminder for session: {}", session.getId(), e);
            }
        }

        if (remindersSent > 0) {
            log.info("Sent {} session reminders", remindersSent);
        }
    }
}
