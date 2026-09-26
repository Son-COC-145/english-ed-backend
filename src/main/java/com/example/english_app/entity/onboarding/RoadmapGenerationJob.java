package com.example.english_app.entity.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "roadmap_generation_jobs",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roadmap_job_student_version",
                columnNames = {"student_id", "generation_version"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoadmapGenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "generation_version", nullable = false)
    private Integer generationVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false, length = 10)
    private CefrLevel cefrLevel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "goal_survey_json", columnDefinition = "jsonb")
    private String goalSurveyJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoadmapGenerationStatus status = RoadmapGenerationStatus.PENDING;

    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "available_at", nullable = false)
    private LocalDateTime availableAt;

    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "claim_token", length = 36)
    private String claimToken;

    @Column(name = "last_error", length = 200)
    private String lastError;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
