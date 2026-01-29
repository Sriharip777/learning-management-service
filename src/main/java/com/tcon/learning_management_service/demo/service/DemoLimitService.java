package com.tcon.learning_management_service.demo.service;


import com.tcon.learning_management_service.demo.dto.DemoLimitDto;
import com.tcon.learning_management_service.demo.entity.DemoClassLimit;
import com.tcon.learning_management_service.demo.repository.DemoClassLimitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoLimitService {

    private final DemoClassLimitRepository limitRepository;

    public boolean canBookDemo(String studentId) {
        DemoClassLimit limit = getOrCreateLimit(studentId);
        return limit.getDemosUsed() < limit.getTotalDemosAllowed() && limit.getIsLimitActive();
    }

    public DemoLimitDto getDemoLimit(String studentId) {
        DemoClassLimit limit = getOrCreateLimit(studentId);

        int remaining = Math.max(0, limit.getTotalDemosAllowed() - limit.getDemosUsed());
        boolean canBook = remaining > 0 && limit.getIsLimitActive();

        String message;
        if (!canBook) {
            message = "You have reached the maximum number of demo classes";
        } else {
            message = String.format("You can book %d more demo class(es)", remaining);
        }

        return DemoLimitDto.builder()
                .studentId(studentId)
                .totalDemosAllowed(limit.getTotalDemosAllowed())
                .demosUsed(limit.getDemosUsed())
                .demosRemaining(remaining)
                .canBookDemo(canBook)
                .message(message)
                .build();
    }

    @Transactional
    public void incrementDemoUsage(String studentId) {
        DemoClassLimit limit = getOrCreateLimit(studentId);

        limit.setDemosUsed(limit.getDemosUsed() + 1);

        if (limit.getFirstDemoAt() == null) {
            limit.setFirstDemoAt(LocalDateTime.now());
        }

        limit.setLastDemoAt(LocalDateTime.now());

        limitRepository.save(limit);
        log.info("Demo usage incremented for student: {}. Total used: {}",
                studentId, limit.getDemosUsed());
    }

    @Transactional
    public void resetDemoLimit(String studentId) {
        DemoClassLimit limit = getOrCreateLimit(studentId);

        limit.setDemosUsed(0);
        limit.setResetAt(LocalDateTime.now());

        limitRepository.save(limit);
        log.info("Demo limit reset for student: {}", studentId);
    }

    private DemoClassLimit getOrCreateLimit(String studentId) {
        return limitRepository.findByStudentId(studentId)
                .orElseGet(() -> {
                    DemoClassLimit newLimit = DemoClassLimit.builder()
                            .studentId(studentId)
                            .totalDemosAllowed(3)
                            .demosUsed(0)
                            .isLimitActive(true)
                            .build();
                    return limitRepository.save(newLimit);
                });
    }
    @Transactional
    public DemoLimitDto initializeDemoLimit(String studentId) {
        log.info("Initializing demo limit for new student: {}", studentId);

        // Check if limit already exists
        if (limitRepository.findByStudentId(studentId).isPresent()) {
            log.warn("Demo limit already exists for student: {}", studentId);
            return getDemoLimit(studentId); // Return existing
        }

        // Create new limit with 3 demos
        DemoClassLimit newLimit = DemoClassLimit.builder()
                .studentId(studentId)
                .totalDemosAllowed(3)
                .demosUsed(0)
                .isLimitActive(true)
                .build();

        limitRepository.save(newLimit);
        log.info("Demo limit initialized for student: {} with {} demos", studentId, 3);

        return getDemoLimit(studentId);
    }

}
