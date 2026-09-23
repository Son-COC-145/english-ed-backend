package com.example.english_app.entity.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "placement_test_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlacementTestSession {

    /** Session timeout: 30 phút không hoạt động sẽ bị hết hạn */
    public static final int SESSION_TIMEOUT_MINUTES = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "current_question_index", nullable = false)
    @Builder.Default
    private Integer currentQuestionIndex = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_cefr_estimate")
    private CefrLevel currentCefrEstimate;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    /**
     * Số lần trả lời sai liên tiếp tính đến câu hỏi hiện tại.
     * Reset về 0 khi trả lời đúng, tăng 1 khi trả lời sai.
     * Được lưu trực tiếp để tránh N+1 query (không cần load toàn bộ answers để đếm).
     */
    @Column(name = "current_wrong_streak", nullable = false)
    @Builder.Default
    private Integer currentWrongStreak = 0;

    // ── Per-skill adaptive CEFR estimates ─────────────────────────────────────
    // Mỗi skill có estimate độc lập, bắt đầu từ A2, cập nhật sau mỗi câu trả lời.
    // current_cefr_estimate (cũ) không còn được write — giữ nguyên trong DB để compat.

    @Enumerated(EnumType.STRING)
    @Column(name = "vocab_cefr_estimate")
    @Builder.Default
    private CefrLevel vocabCefrEstimate = CefrLevel.A2;

    @Enumerated(EnumType.STRING)
    @Column(name = "grammar_cefr_estimate")
    @Builder.Default
    private CefrLevel grammarCefrEstimate = CefrLevel.A2;

    @Enumerated(EnumType.STRING)
    @Column(name = "reading_cefr_estimate")
    @Builder.Default
    private CefrLevel readingCefrEstimate = CefrLevel.A2;

    @Enumerated(EnumType.STRING)
    @Column(name = "listening_cefr_estimate")
    @Builder.Default
    private CefrLevel listeningCefrEstimate = CefrLevel.A2;

    @Enumerated(EnumType.STRING)
    @Column(name = "pronunciation_cefr_estimate")
    @Builder.Default
    private CefrLevel pronunciationCefrEstimate = CefrLevel.A2;

    /**
     * Kiểm tra session có hết hạn chưa (quá SESSION_TIMEOUT_MINUTES không hoạt động).
     * Đây là single source of truth cho timeout logic — dùng ở cả OnboardingService
     * và PronunciationService thay vì hardcode magic number ở từng chỗ.
     *
     * @return true nếu session đã timeout và không còn dùng được.
     */
    public boolean isExpired() {
        LocalDateTime reference = lastActivityAt != null ? lastActivityAt : startedAt;
        if (reference == null) return false;
        return reference.plusMinutes(SESSION_TIMEOUT_MINUTES).isBefore(LocalDateTime.now());
    }
}

