package com.example.english_app.event;

import lombok.Getter;

/**
 * Event được publish sau khi lưu PronunciationPracticeLog thành công.
 *
 * <p>Thiết kế bất biến (immutable) để an toàn khi truyền qua thread boundary (@Async).
 * logId được đưa vào để Cron Job trong DLQ có thể trace-back khi cần reprocess.
 */
@Getter
public class PronunciationCompletedEvent {

    private final Long studentId;
    private final int  xpReward;  // 10-20 XP tuỳ ngưỡng điểm overall
    private final Long logId;     // FK tới pronunciation_practice_logs.id

    public PronunciationCompletedEvent(Long studentId, int xpReward, Long logId) {
        this.studentId = studentId;
        this.xpReward  = xpReward;
        this.logId     = logId;
    }
}
