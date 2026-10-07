package com.example.english_app.entity.adaptive;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationPriority;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemStatus;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "today_plan_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_today_plan_recommendation",
                columnNames = {"plan_id", "recommendation_id"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayPlanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plan_id", nullable = false)
    private Long planId;

    @Column(name = "recommendation_id", nullable = false, length = 64)
    private String recommendationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TodayPlanItemType type;

    @Column(length = 250)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LearnerSkill skill;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "target_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode target;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "navigation_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode navigation;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason_code", nullable = false, length = 40)
    private RecommendationReasonCode reasonCode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reason_params", nullable = false, columnDefinition = "jsonb")
    private JsonNode reasonParams;

    @Enumerated(EnumType.STRING)
    @Column(length = 2)
    private RecommendationPriority priority;

    @Column(name = "estimated_minutes", nullable = false)
    private Integer estimatedMinutes;

    @Builder.Default
    @Column(name = "completed_units", nullable = false)
    private Integer completedUnits = 0;

    @Builder.Default
    @Column(name = "total_units", nullable = false)
    private Integer totalUnits = 1;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TodayPlanItemStatus status = TodayPlanItemStatus.TODO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "source_snapshot", nullable = false, columnDefinition = "jsonb")
    private JsonNode sourceSnapshot;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "last_included_revision", nullable = false)
    private Integer lastIncludedRevision;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
