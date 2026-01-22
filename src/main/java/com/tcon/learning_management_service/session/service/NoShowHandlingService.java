package com.tcon.learning_management_service.session.service;

import com.tcon.learning_management_service.session.entity.ClassSession;
import com.tcon.learning_management_service.session.entity.ClassStatus;
import com.tcon.learning_management_service.session.repository.ClassSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NoShowHandlingService {

    private final ClassSessionRepository sessionRepository;

    /**
     * Runs every 15 minutes to check for no-show sessions
     */
    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void checkForNoShows() {
        log.info("Checking for no-show sessions");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.minusMinutes(30); // 30 minutes grace period

        // Find scheduled sessions that should have started 30 minutes ago
        List<ClassSession> potentialNoShows = sessionRepository
                .findByStatusAndScheduledStartTimeBefore(ClassStatus.SCHEDULED, threshold);

        int noShowCount = 0;
        for (ClassSession session : potentialNoShows) {
            if (session.getActualStartTime() == null) {
                session.setStatus(ClassStatus.NO_SHOW);
                sessionRepository.save(session);
                noShowCount++;
                log.warn("Session marked as no-show: {} - Scheduled at: {}",
                        session.getId(), session.getScheduledStartTime());
            }
        }

        if (noShowCount > 0) {
            log.info("Marked {} sessions as no-show", noShowCount);
        }
    }

    @Transactional
    public void markAsNoShow(String sessionId, String teacherId) {
        log.info("Manually marking session as no-show: {}", sessionId);

        ClassSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (!session.getTeacherId().equals(teacherId)) {
            throw new IllegalArgumentException("Unauthorized: Teacher does not own this session");
        }

        session.setStatus(ClassStatus.NO_SHOW);
        sessionRepository.save(session);

        log.info("Session marked as no-show: {}", sessionId);
    }
}
