package com.example.english_app.entity.adaptive;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearnerEvidenceSource;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.MasteryTrend;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "learner_skill_states",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learner_skill_state",
                columnNames = {"student_id", "skill"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerSkillState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearnerSkill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", length = 2)
    private CefrLevel cefrLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LearnerEvidenceSource source;

    @Column(precision = 5, scale = 2)
    private BigDecimal mastery;

    @Column(name = "recent_score", precision = 5, scale = 2)
    private BigDecimal recentScore;

    @Builder.Default
    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal confidence = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "evidence_weight", nullable = false, precision = 8, scale = 3)
    private BigDecimal evidenceWeight = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "evidence_count", nullable = false)
    private Integer evidenceCount = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MasteryTrend trend = MasteryTrend.STABLE;

    @Column(name = "last_practiced_at")
    private LocalDateTime lastPracticedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
