package com.example.english_app.service.onboarding;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Opt-in migration and constraint verification against a disposable local PostgreSQL database. */
@EnabledIfEnvironmentVariable(
        named = "ONBOARDING_TEST_JDBC_URL",
        matches = "jdbc:postgresql://127\\.0\\.0\\.1:55440/postgres")
class OnboardingPostgresIntegrationTest {

    private static JdbcTemplate jdbc;

    @BeforeAll
    static void migrateCleanDatabase() {
        String url = System.getenv("ONBOARDING_TEST_JDBC_URL");
        String user = System.getenv().getOrDefault("ONBOARDING_TEST_DB_USER", "postgres");
        String password = System.getenv().getOrDefault("ONBOARDING_TEST_DB_PASSWORD", "postgres");
        Flyway.configure()
                .dataSource(url, user, password)
                .locations("filesystem:src/main/resources/db/migration")
                .load()
                .migrate();
        jdbc = new JdbcTemplate(new DriverManagerDataSource(url, user, password));
    }

    @Test
    void onboardingMigrationsProduceExpectedContract() {
        assertThat(columnExists("questions", "placement_audio_url")).isTrue();
        assertThat(columnExists("placement_test_sessions", "progress_percent")).isTrue();
        assertThat(columnExists("placement_test_sessions", "confidence_score")).isFalse();
        assertThat(columnExists("placement_test_answers", "submission_id")).isTrue();
        assertThat(columnExists("placement_test_answers", "pronunciation_overall_score")).isTrue();
        assertThat(columnExists("users", "onboarding_completed")).isTrue();
        assertThat(tableExists("roadmap_generation_jobs")).isTrue();
        assertThat(tableExists("placement_pronunciation_submissions")).isTrue();
        assertThat(tableExists("roadmap_module_progress")).isTrue();

        Boolean dailyGoalNullable = jdbc.queryForObject("""
                SELECT is_nullable = 'YES'
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'student_onboarding'
                  AND column_name = 'daily_goal_xp'
                """, Boolean.class);
        assertThat(dailyGoalNullable).isTrue();
    }

    @Test
    void databaseRejectsDuplicateAnswerAndConcurrentPronunciationClaims() {
        Long studentId = jdbc.queryForObject(
                "SELECT id FROM users WHERE role='STUDENT' ORDER BY id LIMIT 1", Long.class);
        Long firstQuestion = jdbc.queryForObject(
                "SELECT id FROM questions ORDER BY id LIMIT 1", Long.class);
        Long secondQuestion = jdbc.queryForObject(
                "SELECT id FROM questions WHERE id <> ? ORDER BY id LIMIT 1", Long.class, firstQuestion);
        Long sessionId = jdbc.queryForObject("""
                INSERT INTO placement_test_sessions
                    (current_question_index, is_completed, last_activity_at, started_at, student_id)
                VALUES (0, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, ?)
                RETURNING id
                """, Long.class, studentId);

        UUID submissionId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO placement_test_answers
                    (answer_given, answered_at, is_correct, question_id, session_id,
                     submission_id, request_hash, submission_type)
                VALUES ('A', CURRENT_TIMESTAMP, true, ?, ?, ?, ?, 'ANSWER')
                """, firstQuestion, sessionId, submissionId, "a".repeat(64));

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO placement_test_answers
                    (answer_given, answered_at, is_correct, question_id, session_id,
                     submission_id, request_hash, submission_type)
                VALUES ('B', CURRENT_TIMESTAMP, false, ?, ?, ?, ?, 'ANSWER')
                """, secondQuestion, sessionId, submissionId, "b".repeat(64)))
                .isInstanceOf(DataAccessException.class);

        UUID audioSubmission = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO placement_pronunciation_submissions
                    (session_id, question_id, submission_id, request_hash, status)
                VALUES (?, ?, ?, ?, 'PROCESSING')
                """, sessionId, secondQuestion, audioSubmission, "c".repeat(64));

        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO placement_pronunciation_submissions
                    (session_id, question_id, submission_id, request_hash, status)
                VALUES (?, ?, ?, ?, 'PROCESSING')
                """, sessionId, secondQuestion, UUID.randomUUID(), "d".repeat(64)))
                .isInstanceOf(DataAccessException.class);
    }

    private static boolean columnExists(String table, String column) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.columns
                    WHERE table_schema='public' AND table_name=? AND column_name=?
                )
                """, Boolean.class, table, column);
        return Boolean.TRUE.equals(exists);
    }

    private static boolean tableExists(String table) {
        Boolean exists = jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM information_schema.tables
                    WHERE table_schema='public' AND table_name=?
                )
                """, Boolean.class, table);
        return Boolean.TRUE.equals(exists);
    }
}
