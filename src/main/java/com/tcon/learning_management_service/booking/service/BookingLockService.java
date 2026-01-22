package com.tcon.learning_management_service.booking.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class BookingLockService {

    private final Map<String, LockEntry> locks = new ConcurrentHashMap<>();
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    public boolean acquireLock(String resourceId, String userId) {
        cleanupExpiredLocks();

        LockEntry existingLock = locks.get(resourceId);

        if (existingLock != null) {
            if (existingLock.getUserId().equals(userId)) {
                // Same user trying to acquire lock again
                existingLock.setAcquiredAt(LocalDateTime.now());
                return true;
            }

            // Lock held by another user
            if (!isLockExpired(existingLock)) {
                log.warn("Lock for {} is held by another user: {}", resourceId, existingLock.getUserId());
                return false;
            }

            // Lock expired, remove it
            locks.remove(resourceId);
        }

        // Acquire new lock
        LockEntry newLock = new LockEntry(userId, LocalDateTime.now());
        locks.put(resourceId, newLock);
        log.info("Lock acquired for {} by user {}", resourceId, userId);
        return true;
    }

    public void releaseLock(String resourceId, String userId) {
        LockEntry lock = locks.get(resourceId);

        if (lock != null && lock.getUserId().equals(userId)) {
            locks.remove(resourceId);
            log.info("Lock released for {} by user {}", resourceId, userId);
        }
    }

    private boolean isLockExpired(LockEntry lock) {
        return Duration.between(lock.getAcquiredAt(), LocalDateTime.now()).compareTo(LOCK_TIMEOUT) > 0;
    }

    private void cleanupExpiredLocks() {
        locks.entrySet().removeIf(entry -> isLockExpired(entry.getValue()));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LockEntry {
        private String userId;
        private LocalDateTime acquiredAt;
    }
}

