package com.example.english_app.service.ipa;

import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.ipa.FailedJob;
import com.example.english_app.event.PronunciationCompletedEvent;
import com.example.english_app.repository.StudentStatRepository;
import com.example.english_app.repository.ipa.FailedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bean riêng chứa @Retryable + @Transactional(REQUIRES_NEW) để cộng XP.
 *
 * <p><b>Tại sao REQUIRES_NEW?</b>
 * Method này chạy trên thread từ @Async — không có TX nào đang active.
 * REQUIRES_NEW tạo TX hoàn toàn mới, độc lập với publisher ban đầu.
 * Nếu Retry thất bại hoàn toàn, @Recover ghi vào bảng failed_jobs (DLQ).
 *
 * <p><b>Cơ chế 2 lớp:</b>
 * - Lớp 1: @Retryable retry 3 lần với exponential backoff (1s → 2s → 4s).
 * - Lớp 2: @Recover ghi failed_jobs → Cron Job xử lý lại lúc 2h sáng.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetryableGamificationService {

    private final StudentStatRepository studentStatRepository;
    private final FailedJobRepository   failedJobRepository;

    @Retryable(
            retryFor  = { TransientDataAccessException.class, CannotAcquireLockException.class },
            maxAttempts = 3,
            backoff   = @Backoff(delay = 1000, multiplier = 2)  // 1s → 2s → 4s
    )
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addXp(PronunciationCompletedEvent event) {
        StudentStat stat = studentStatRepository
                .findByStudentId(event.getStudentId())
                .orElseGet(() -> {
                    log.warn("StudentStat not found for studentId={} — skipping XP add", event.getStudentId());
                    return null;
                });

        if (stat == null) return;

        stat.setTotalXp(stat.getTotalXp() + event.getXpReward());
        studentStatRepository.save(stat);

        log.info("XP added: studentId={}, +{}xp, logId={}",
                event.getStudentId(), event.getXpReward(), event.getLogId());
    }

    /**
     * Fallback sau 3 lần retry thất bại — ghi vào DLQ để Cron Job xử lý lại.
     * Method signature PHẢI khớp với addXp: cùng tham số, thêm Exception đầu tiên.
     */
    @Recover
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recover(Exception ex, PronunciationCompletedEvent event) {
        log.error("[DLQ] Failed to add {} XP for studentId={} after 3 retries. logId={}. Error: {}",
                event.getXpReward(), event.getStudentId(), event.getLogId(), ex.getMessage());

        try {
            FailedJob failedJob = FailedJob.builder()
                    .studentId(event.getStudentId())
                    .payloadJson(buildPayloadJson(event))
                    .errorMessage(ex.getMessage())
                    .build();
            failedJobRepository.save(failedJob);
        } catch (Exception dlqEx) {
            // DLQ cũng thất bại — chỉ log lại, không throw, tránh crash thread
            log.error("[DLQ-CRITICAL] Could not save to failed_jobs for studentId={}. Manual intervention required!",
                    event.getStudentId(), dlqEx);
        }
    }

    private String buildPayloadJson(PronunciationCompletedEvent event) {
        return String.format(
                "{\"studentId\":%d,\"xpReward\":%d,\"logId\":%d}",
                event.getStudentId(), event.getXpReward(), event.getLogId()
        );
    }
}
