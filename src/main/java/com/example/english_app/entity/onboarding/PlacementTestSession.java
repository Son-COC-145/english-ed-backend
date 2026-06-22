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
}
