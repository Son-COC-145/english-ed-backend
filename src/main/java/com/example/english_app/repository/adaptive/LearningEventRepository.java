package com.example.english_app.repository.adaptive;

import com.example.english_app.entity.adaptive.LearningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface LearningEventRepository extends JpaRepository<LearningEvent, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO learning_events (
                event_id, student_id, event_type, source, source_reference,
                skill, entity_type, entity_id, score, duration_seconds,
                duration_source, payload, schema_version, occurred_at,
                causation_event_id, status, attempt_count, next_retry_at)
            VALUES (
                :eventId, :studentId, :eventType, :source, :sourceReference,
                :skill, :entityType, :entityId, :score, :durationSeconds,
                :durationSource, CAST(:payload AS jsonb), :schemaVersion, :occurredAt,
                :causationEventId, 'PENDING', 0, :nextRetryAt)
            ON CONFLICT (student_id, source, source_reference, event_type) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("eventId") UUID eventId,
            @Param("studentId") Long studentId,
            @Param("eventType") String eventType,
            @Param("source") String source,
            @Param("sourceReference") String sourceReference,
            @Param("skill") String skill,
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("score") Short score,
            @Param("durationSeconds") Integer durationSeconds,
            @Param("durationSource") String durationSource,
            @Param("payload") String payload,
            @Param("schemaVersion") Short schemaVersion,
            @Param("occurredAt") LocalDateTime occurredAt,
            @Param("causationEventId") UUID causationEventId,
            @Param("nextRetryAt") LocalDateTime nextRetryAt);
}

