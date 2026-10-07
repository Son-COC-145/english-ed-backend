package com.example.english_app.entity.adaptive;

import com.example.english_app.entity.enums.CefrLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "learner_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerProfile {

    @Id
    @Column(name = "student_id")
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", length = 2)
    private CefrLevel cefrLevel;

    @Column(name = "cefr_source", length = 20)
    private String cefrSource;

    @Column(name = "cefr_assessed_at")
    private LocalDateTime cefrAssessedAt;

    @Column(name = "overall_mastery", precision = 5, scale = 2)
    private BigDecimal overallMastery;

    @Builder.Default
    @Column(name = "profile_version", nullable = false)
    private Long profileVersion = 0L;

    @Builder.Default
    @Column(name = "total_activities", nullable = false)
    private Integer totalActivities = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
