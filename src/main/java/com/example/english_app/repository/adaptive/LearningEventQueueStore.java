package com.example.english_app.repository.adaptive;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LearningEventQueueStore {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public List<UUID> claimPending(
            String correlationId,
            int batchSize,
            LocalDateTime now) {
        String sql = """
                WITH candidates AS (
                    SELECT event.event_id
                    FROM learning_events event
                    WHERE event.status = 'PENDING'
                      AND event.next_retry_at <= :now
                      AND NOT EXISTS (
                          SELECT 1
                          FROM learning_events processing
                          WHERE processing.student_id = event.student_id
                            AND processing.status = 'PROCESSING')
                      AND NOT EXISTS (
                          SELECT 1
                          FROM learning_events earlier
                          WHERE earlier.student_id = event.student_id
                            AND earlier.status = 'PENDING'
                            AND earlier.seq < event.seq)
                    ORDER BY event.seq
                    LIMIT :batchSize
                    FOR UPDATE SKIP LOCKED
                )
                UPDATE learning_events event
                SET status = 'PROCESSING',
                    correlation_id = :correlationId,
                    processing_started_at = :now
                FROM candidates
                WHERE event.event_id = candidates.event_id
                RETURNING event.event_id
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("correlationId", correlationId)
                .addValue("batchSize", batchSize)
                .addValue("now", now);
        return jdbcTemplate.query(
                sql,
                parameters,
                (resultSet, rowNumber) -> resultSet.getObject("event_id", UUID.class));
    }

    public int markRetryOrFailed(
            UUID eventId,
            String correlationId,
            int maxAttempts,
            int maxBackoffSeconds,
            String error,
            LocalDateTime now) {
        String sql = """
                UPDATE learning_events
                SET attempt_count = attempt_count + 1,
                    status = CASE
                        WHEN attempt_count + 1 >= :maxAttempts THEN 'FAILED'
                        ELSE 'PENDING'
                    END,
                    next_retry_at = CASE
                        WHEN attempt_count + 1 >= :maxAttempts THEN :now
                        ELSE :now + LEAST(
                            :maxBackoffSeconds,
                            CAST(power(2, LEAST(attempt_count + 1, 30)) AS INTEGER)
                        ) * INTERVAL '1 second'
                    END,
                    last_error = :error,
                    correlation_id = NULL,
                    processing_started_at = NULL,
                    processed_at = CASE
                        WHEN attempt_count + 1 >= :maxAttempts THEN :now
                        ELSE NULL
                    END
                WHERE event_id = :eventId
                  AND status = 'PROCESSING'
                  AND correlation_id = :correlationId
                """;
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", eventId)
                .addValue("correlationId", correlationId)
                .addValue("maxAttempts", maxAttempts)
                .addValue("maxBackoffSeconds", maxBackoffSeconds)
                .addValue("error", error)
                .addValue("now", now);
        return jdbcTemplate.update(sql, parameters);
    }

    public int releaseStuck(LocalDateTime stuckBefore, LocalDateTime now) {
        String sql = """
                UPDATE learning_events
                SET status = 'PENDING',
                    next_retry_at = :now,
                    last_error = 'ProcessingTimeout',
                    correlation_id = NULL,
                    processing_started_at = NULL
                WHERE status = 'PROCESSING'
                  AND processing_started_at < :stuckBefore
                """;
        return jdbcTemplate.update(sql, new MapSqlParameterSource()
                .addValue("stuckBefore", stuckBefore)
                .addValue("now", now));
    }
}
