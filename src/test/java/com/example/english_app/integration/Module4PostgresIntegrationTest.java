package com.example.english_app.integration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Opt-in integration suite. It creates and drops its own schema, never uses application credentials.
 */
@EnabledIfEnvironmentVariable(named = "MODULE4_TEST_JDBC_URL", matches = "jdbc:postgresql://.*")
class Module4PostgresIntegrationTest {
    private static DataSource dataSource;
    private static JdbcTemplate jdbc;
    private static String schema;

    @BeforeAll
    static void setUpDatabase() throws Exception {
        String url = System.getenv("MODULE4_TEST_JDBC_URL");
        schema = "module4_test_" + UUID.randomUUID().toString().replace("-", "");
        JdbcTemplate admin = new JdbcTemplate(new DriverManagerDataSource(url,
                System.getenv().getOrDefault("MODULE4_TEST_DB_USER", "phase123"),
                System.getenv().getOrDefault("MODULE4_TEST_DB_PASSWORD", "")));
        admin.execute("CREATE SCHEMA " + schema);
        dataSource = new DriverManagerDataSource(url + "?currentSchema=" + schema,
                System.getenv().getOrDefault("MODULE4_TEST_DB_USER", "phase123"),
                System.getenv().getOrDefault("MODULE4_TEST_DB_PASSWORD", ""));
        try (Connection connection = dataSource.getConnection()) {
            for (String migration : List.of("V1__init.sql", "V10__update_module4_schema.sql",
                    "V29__update_notification_type_check.sql", "V30__add_syllabus_item_topics.sql",
                    "V42__add_cloudinary_metadata_to_teaching_materials.sql")) {
                String sql = Files.readString(Path.of("src/main/resources/db/migration", migration), StandardCharsets.UTF_8)
                        .replace("public.", schema + ".")
                        .replace("SELECT pg_catalog.set_config('search_path', '', false);", "SET search_path TO " + schema + ";");
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8)));
            }
        }
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT INTO users (id,email,full_name,is_active,locale,password_hash,role,provider,created_at,updated_at) " +
                "VALUES (1,'teacher@test.invalid','Teacher',true,'en','unused','TEACHER','LOCAL',now(),now())," +
                "(2,'student@test.invalid','Student',true,'en','unused','STUDENT','LOCAL',now(),now())");
    }

    @AfterAll
    static void tearDownDatabase() {
        if (dataSource != null && schema != null && schema.matches("module4_test_[a-f0-9]{32}")) {
            new JdbcTemplate(dataSource).execute("DROP SCHEMA " + schema + " CASCADE");
        }
    }

    @Test
    void enrollmentUniqueConstraintRejectsDuplicateStudentInCourse() {
        jdbc.update("INSERT INTO classes (id,name,teacher_id,is_active,created_at) VALUES (10,'A1',1,true,now())");
        jdbc.update("INSERT INTO class_students (class_id,student_id,status,joined_at) VALUES (10,2,'ACTIVE',now())");

        assertThrows(Exception.class, () -> jdbc.update(
                "INSERT INTO class_students (class_id,student_id,status,joined_at) VALUES (10,2,'ACTIVE',now())"));
    }

    @Test
    void outboxIdempotencyAndNotificationLinkAreUnique() {
        jdbc.update("INSERT INTO notification_outbox (idempotency_key,recipient_id,notification_type,title,status,available_at) " +
                "VALUES ('assignment-created:1:2',2,'ASSIGNMENT','New assignment','PENDING',now())");
        Long eventId = jdbc.queryForObject("SELECT id FROM notification_outbox WHERE idempotency_key='assignment-created:1:2'", Long.class);
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO notification_outbox " +
                "(idempotency_key,recipient_id,notification_type,title,status,available_at) " +
                "VALUES ('assignment-created:1:2',2,'ASSIGNMENT','Duplicate','PENDING',now())"));

        jdbc.update("INSERT INTO notifications (user_id,type,title,body,is_read,outbox_event_id,created_at) " +
                "VALUES (2,'ASSIGNMENT','New assignment','Body',false,?,now())", eventId);
        assertThrows(Exception.class, () -> jdbc.update("INSERT INTO notifications " +
                "(user_id,type,title,body,is_read,outbox_event_id,created_at) VALUES (2,'ASSIGNMENT','Duplicate','Body',false,?,now())", eventId));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM notifications WHERE outbox_event_id=?", Integer.class, eventId));
    }
}
