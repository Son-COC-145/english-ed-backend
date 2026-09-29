package com.example.english_app.service.ipa;

import com.example.english_app.event.PronunciationCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener nhận PronunciationCompletedEvent và dispatch sang RetryableGamificationService.
 *
 * <p><b>⚠️ Tại sao tách 2 Bean?</b>
 * @Retryable hoạt động qua AOP Proxy. Nếu gắn @Retryable trực tiếp lên method
 * trong cùng Bean với @Async, Spring bypass Proxy → Retry không bao giờ chạy.
 * Tách sang Bean riêng đảm bảo gọi qua Bean boundary → AOP Proxy hoạt động đúng.
 *
 * <p><b>Transaction:</b> Method này KHÔNG có @Transactional.
 * @Async tạo thread mới — thread đó không kế thừa TX của publisher.
 * TX được quản lý hoàn toàn trong RetryableGamificationService (REQUIRES_NEW).
 *
 * <p><b>Chỉ chạy sau khi commit:</b> event được phát bên trong TX của
 * {@code IpaPronunciationServiceImpl.assess}. Với {@code AFTER_COMMIT}, XP chỉ được cộng khi log luyện phát âm
 * đã lưu thành công; TX bị rollback thì event bị bỏ, không cộng XP cho kết quả không tồn tại.
 * {@code fallbackExecution = true}: nếu event được phát ngoài TX thì vẫn xử lý ngay.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PronunciationEventListener {

    private final RetryableGamificationService retryableGamificationService;

    @Async        // Thread mới — không kế thừa TX của publisher
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handle(PronunciationCompletedEvent event) {
        log.debug("Received PronunciationCompletedEvent: studentId={}, xp={}, logId={}",
                event.getStudentId(), event.getXpReward(), event.getLogId());
        // Gọi qua Bean boundary → AOP Proxy áp dụng @Retryable đúng
        retryableGamificationService.addXp(event);
    }
}
