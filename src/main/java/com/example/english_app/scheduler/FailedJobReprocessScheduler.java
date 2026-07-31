package com.example.english_app.scheduler;

import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.ipa.FailedJob;
import com.example.english_app.repository.StudentStatRepository;
import com.example.english_app.repository.ipa.FailedJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cron Job chạy lúc 2:00 AM hàng ngày để reprocess các job XP bị lỗi.
 *
 * <p><b>Fault Tolerance Layer 2:</b> Sau khi @Retryable thất bại 3 lần, event được ghi vào
 * bảng failed_jobs với status=PENDING. Job này quét bảng đó và thử cộng XP lại.
 * Nếu vẫn thất bại sau khi Cron xử lý, status được đổi thành DEAD để tránh loop vô tận.
 *
 * <p><b>Idempotent Design:</b> Mỗi job chỉ được xử lý một lần (status PENDING → PROCESSED/DEAD).
 * Ngay cả khi Cron chạy lại do restart, các job đã PROCESSED sẽ bị bỏ qua.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FailedJobReprocessScheduler {

    private final FailedJobRepository   failedJobRepository;
    private final StudentStatRepository studentStatRepository;

    /**
     * Chạy lúc 02:00 AM mỗi ngày — cron expression: "0 0 2 * * *"
     * Giờ thấp điểm để không ảnh hưởng UX.
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void reprocessFailedJobs() {
        List<FailedJob> pendingJobs = failedJobRepository.findByStatus("PENDING");

        if (pendingJobs.isEmpty()) {
            log.debug("[DLQ-Cron] No pending failed jobs to reprocess.");
            return;
        }

        log.info("[DLQ-Cron] Starting reprocess of {} pending failed job(s).", pendingJobs.size());

        int successCount = 0;
        int failCount    = 0;

        for (FailedJob job : pendingJobs) {
            try {
                processSingleJob(job);
                successCount++;
            } catch (Exception e) {
                log.error("[DLQ-Cron] Failed to reprocess jobId={}, studentId={}: {}",
                        job.getId(), job.getStudentId(), e.getMessage());
                markAsDead(job, e.getMessage());
                failCount++;
            }
        }

        log.info("[DLQ-Cron] Reprocess completed. success={}, failed/dead={}", successCount, failCount);
    }

    @Transactional
    protected void processSingleJob(FailedJob job) {
        Long studentId = job.getStudentId();

        // Parse xpReward từ payload JSON — simple string parsing, không cần ObjectMapper
        int xpReward = extractXpFromPayload(job.getPayloadJson());

        StudentStat stat = studentStatRepository.findByStudentId(studentId)
                .orElse(null);

        if (stat == null) {
            log.warn("[DLQ-Cron] StudentStat not found for studentId={}, marking as DEAD.", studentId);
            markAsDead(job, "StudentStat not found");
            return;
        }

        stat.setTotalXp(stat.getTotalXp() + xpReward);
        studentStatRepository.save(stat);

        // Mark as PROCESSED
        job.setStatus("PROCESSED");
        job.setRetryCount((short) (job.getRetryCount() + 1));
        job.setProcessedAt(LocalDateTime.now());
        failedJobRepository.save(job);

        log.info("[DLQ-Cron] Successfully reprocessed jobId={}, added {}xp to studentId={}",
                job.getId(), xpReward, studentId);
    }

    @Transactional
    protected void markAsDead(FailedJob job, String reason) {
        job.setStatus("DEAD");
        job.setErrorMessage(reason);
        job.setRetryCount((short) (job.getRetryCount() + 1));
        failedJobRepository.save(job);
    }

    /**
     * Parse xpReward từ payload JSON string.
     * Payload format: {"studentId":1,"xpReward":15,"logId":42}
     */
    private int extractXpFromPayload(String payloadJson) {
        try {
            // Simple string search — tránh dependency ObjectMapper ở Scheduler layer
            int idx = payloadJson.indexOf("\"xpReward\":");
            if (idx == -1) return 10; // default fallback
            String sub = payloadJson.substring(idx + 11);
            int endIdx = sub.indexOf(',');
            if (endIdx == -1) endIdx = sub.indexOf('}');
            return Integer.parseInt(sub.substring(0, endIdx).trim());
        } catch (Exception e) {
            log.warn("[DLQ-Cron] Could not parse xpReward from payload '{}', defaulting to 10", payloadJson);
            return 10;
        }
    }
}
