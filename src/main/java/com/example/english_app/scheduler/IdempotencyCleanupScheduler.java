package com.example.english_app.scheduler;

import com.example.english_app.repository.gamification.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Tự động xóa idempotency keys hết hạn (> 24h) để tránh table phình to.
 * Chạy lúc 3:00 AM hàng ngày.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupScheduler {

    private final IdempotencyKeyRepository idempotencyKeyRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredKeys() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        idempotencyKeyRepository.deleteByCreatedAtBefore(cutoff);
        log.info("[IdempotencyCleanup] Đã xóa idempotency keys hết hạn trước {}", cutoff);
    }
}
