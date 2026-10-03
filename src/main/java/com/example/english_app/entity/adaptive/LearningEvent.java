package com.example.english_app.entity.adaptive;

import com.example.english_app.entity.enums.DurationSource;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningEventSource;
import com.example.english_app.entity.enums.LearningEventStatus;
import com.example.english_app.entity.enums.LearningEventType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "learning_events")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(nullable = false, insertable = false, updatable = false)
    private Long seq;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50, updatable = false)
    private LearningEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40, updatable = false)
    private LearningEventSource source;

    @Column(name = "source_reference", nullable = false, length = 120, updatable = false)
    private String sourceReference;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, updatable = false)
    private LearnerSkill skill;

    @Column(name = "entity_type", length = 30, updatable = false)
    private String entityType;

    @Column(name = "entity_id", updatable = false)
    private Long entityId;

    @Column(updatable = false)
    private Short score;

    @Column(name = "duration_seconds", updatable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_source", length = 10, updatable = false)
    private DurationSource durationSource;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb", updatable = false)
    private JsonNode payload;

    @Column(name = "schema_version", nullable = false, updatable = false)
    private Short schemaVersion;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(name = "causation_event_id", updatable = false)
    private UUID causationEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private LearningEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;

    @Column(name = "next_retry_at", nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;
}

