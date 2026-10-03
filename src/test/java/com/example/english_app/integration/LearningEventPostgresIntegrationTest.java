package com.example.english_app.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import com.example.english_app.repository.adaptive.LearningEventQueueStore;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EnabledIfEnvironmentVariable(
        named = "ONBOARDING_TEST_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:55440/postgres")
class LearningEventPostgresIntegrationTest {

    private static JdbcTemplate jdbc;
    private static LearningEventQueueStore queueStore;

    @BeforeAll
    static void migrateDatabase() {
        String url = System.getenv("ONBOARDING_TEST_JDBC_URL");
        String user = System.getenv().getOrDefault("ONBOARDING_TEST_DB_USER", "postgres");
        String password = System.getenv().getOrDefault("ONBOARDING_TEST_DB_PASSWORD", "postgres");
        Flyway.configure()
                .dataSource(url, user, password)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        jdbc = new JdbcTemplate(dataSource);
        queueStore = new LearningEventQueueStore(new NamedParameterJdbcTemplate(dataSource));
    }

    @BeforeEach
    void removePreviousLearningEventFixtures() {
        jdbc.update("DELETE FROM learning_events WHERE source_reference LIKE 'it-learning-%'");
    }

    @Test
    void duplicateBusinessEventIsInsertedOnlyOnce() {
        Long studentId = jdbc.queryForObject(
                "SELECT id FROM users WHERE role='STUDENT' ORDER BY id LIMIT 1", Long.class);
        String sourceReference = "it-learning-duplicate-" + UUID.randomUUID();
        String sql = """
                INSERT INTO learning_events (
                    event_id, student_id, event_type, source, source_reference,
                    payload, occurred_at, next_retry_at)
                VALUES (?, ?, 'VOCAB_REVIEWED', 'VOCAB_REVIEW', ?,
                    '{}'::jsonb, now(), now())
                ON CONFLICT (student_id, source, source_reference, event_type) DO NOTHING
                """;

        assertThat(jdbc.update(sql, UUID.randomUUID(), studentId, sourceReference)).isEqualTo(1);
        assertThat(jdbc.update(sql, UUID.randomUUID(), studentId, sourceReference)).isZero();
        Integer count = jdbc.queryForObject("""
                SELECT count(*) FROM learning_events
                WHERE student_id=? AND source='VOCAB_REVIEW'
                  AND source_reference=?
                  AND event_type='VOCAB_REVIEWED'
                """, Integer.class, studentId, sourceReference);
        assertThat(count).isEqualTo(1);
    }

    @Test
    void claimsOnlyOldestPendingEventForAStudent() {
        Long studentId = firstStudentId();
        UUID first = insertPending(studentId, "it-learning-claim-first-" + UUID.randomUUID());
        UUID second = insertPending(studentId, "it-learning-claim-second-" + UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();

        List<UUID> firstClaim = queueStore.claimPending("claim-1", 10, now);

        assertThat(firstClaim).contains(first).doesNotContain(second);
        jdbc.update("""
                UPDATE learning_events
                SET status='DONE', correlation_id=NULL, processing_started_at=NULL, processed_at=now()
                WHERE event_id=?
                """, first);

        assertThat(queueStore.claimPending("claim-2", 10, LocalDateTime.now()))
                .contains(second);
    }

    @Test
    void failureRequeuesThenBecomesFailedAtMaximumAttempts() {
        Long studentId = firstStudentId();
        UUID eventId = insertPending(studentId, "it-learning-retry-" + UUID.randomUUID());
        LocalDateTime now = LocalDateTime.now();
        assertThat(queueStore.claimPending("retry-claim-1", 1, now)).contains(eventId);

        queueStore.markRetryOrFailed(
                eventId, "retry-claim-1", 2, 300,
                "IllegalStateException", now);
        assertThat(status(eventId)).isEqualTo("PENDING");
        jdbc.update("UPDATE learning_events SET next_retry_at=now() WHERE event_id=?", eventId);
        assertThat(queueStore.claimPending("retry-claim-2", 1, LocalDateTime.now()))
                .contains(eventId);

        queueStore.markRetryOrFailed(
                eventId, "retry-claim-2", 2, 300,
                "IllegalStateException", LocalDateTime.now());

        assertThat(status(eventId)).isEqualTo("FAILED");
    }

    @Test
    void releasesStuckProcessingEvent() {
        Long studentId = firstStudentId();
        UUID eventId = insertPending(studentId, "it-learning-stuck-" + UUID.randomUUID());
        jdbc.update("""
                UPDATE learning_events
                SET status='PROCESSING', correlation_id='stuck-claim',
                    processing_started_at=now() - interval '11 minutes'
                WHERE event_id=?
                """, eventId);

        int released = queueStore.releaseStuck(
                LocalDateTime.now().minusMinutes(10), LocalDateTime.now());

        assertThat(released).isGreaterThanOrEqualTo(1);
        assertThat(status(eventId)).isEqualTo("PENDING");
    }

    private Long firstStudentId() {
        return jdbc.queryForObject(
                "SELECT id FROM users WHERE role='STUDENT' ORDER BY id LIMIT 1", Long.class);
    }

    private UUID insertPending(Long studentId, String sourceReference) {
        UUID eventId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO learning_events (
                    event_id, student_id, event_type, source, source_reference,
                    payload, occurred_at, next_retry_at)
                VALUES (?, ?, 'VOCAB_REVIEWED', 'VOCAB_REVIEW', ?,
                    '{}'::jsonb, now(), now())
                """, eventId, studentId, sourceReference);
        return eventId;
    }

    private String status(UUID eventId) {
        return jdbc.queryForObject(
                "SELECT status FROM learning_events WHERE event_id=?", String.class, eventId);
    }
}
