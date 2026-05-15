package com.tcon.learning_management_service.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class BookingLockService {

    private final Map<String, LockEntry> locks = new ConcurrentHashMap<>();
    private static final Duration LOCK_TIMEOUT = Duration.ofMinutes(5);

    public boolean acquireLock(String resourceId, String userId) {
        cleanupExpiredLocks();

        AtomicBoolean acquired = new AtomicBoolean(false);

        locks.compute(resourceId, (key, existingLock) -> {
            Instant now = Instant.now();

            if (existingLock == null) {
                acquired.set(true);
                log.info("Lock acquired for {} by user {}", resourceId, userId);
                return new LockEntry(userId, now);
            }

            if (existingLock.getUserId().equals(userId)) {
                existingLock.setAcquiredAt(now);
                acquired.set(true);
                return existingLock;
            }

            if (isLockExpired(existingLock, now)) {
                acquired.set(true);
                log.info("Expired lock replaced for {} by user {}", resourceId, userId);
                return new LockEntry(userId, now);
            }

            log.warn("Lock for {} is held by another user: {}", resourceId, existingLock.getUserId());
            return existingLock;
        });

        return acquired.get();
    }

    public void releaseLock(String resourceId, String userId) {
        locks.computeIfPresent(resourceId, (key, lock) -> {
            if (lock.getUserId().equals(userId)) {
                log.info("Lock released for {} by user {}", resourceId, userId);
                return null;
            }
            return lock;
        });
    }

    private boolean isLockExpired(LockEntry lock) {
        return isLockExpired(lock, Instant.now());
    }

    private boolean isLockExpired(LockEntry lock, Instant now) {
        return Duration.between(lock.getAcquiredAt(), now).compareTo(LOCK_TIMEOUT) > 0;
    }

    private void cleanupExpiredLocks() {
        Instant now = Instant.now();
        locks.entrySet().removeIf(entry -> isLockExpired(entry.getValue(), now));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class LockEntry {
        private String userId;
        private Instant acquiredAt;
    }
}