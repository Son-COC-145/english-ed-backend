package com.example.english_app.entity.adaptive;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "today_plans",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_today_plan_student_date",
                columnNames = {"student_id", "plan_date", "timezone"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "plan_date", nullable = false)
    private LocalDate planDate;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Builder.Default
    @Column(nullable = false)
    private Integer revision = 1;

    @Builder.Default
    @Column(name = "profile_version", nullable = false)
    private Long profileVersion = 0L;

    @Column(name = "rules_version", nullable = false, length = 40)
    private String rulesVersion;

    @Column(name = "budget_minutes", nullable = false)
    private Integer budgetMinutes;

    @Builder.Default
    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes = 0;

    @Builder.Default
    @Column(name = "completed_minutes", nullable = false)
    private Integer completedMinutes = 0;

    @Builder.Default
    @Column(name = "progress_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal progressPercent = BigDecimal.ZERO;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Builder.Default
    @Column(nullable = false)
    private Boolean dirty = false;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}
