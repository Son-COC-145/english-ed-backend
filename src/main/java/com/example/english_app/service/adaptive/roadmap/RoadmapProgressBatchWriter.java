package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.entity.enums.RoadmapModuleStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RoadmapProgressBatchWriter {

    private static final String UPSERT_SQL = """
            INSERT INTO roadmap_module_progress (
                student_id, roadmap_version, week_number, module_index,
                module_key, module_type, topic_id, done_count, total_count,
                status, completed_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (student_id, roadmap_version, module_key)
            DO UPDATE SET
                week_number = EXCLUDED.week_number,
                module_index = EXCLUDED.module_index,
                module_type = EXCLUDED.module_type,
                topic_id = EXCLUDED.topic_id,
                done_count = LEAST(
                    roadmap_module_progress.total_count,
                    GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count)),
                total_count = roadmap_module_progress.total_count,
                status = CASE
                    WHEN roadmap_module_progress.status = 'COMPLETED' THEN 'COMPLETED'
                    WHEN LEAST(
                            roadmap_module_progress.total_count,
                            GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count))
                         = roadmap_module_progress.total_count THEN 'COMPLETED'
                    WHEN GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count) > 0
                         THEN 'IN_PROGRESS'
                    ELSE 'NOT_STARTED'
                END,
                completed_at = CASE
                    WHEN roadmap_module_progress.completed_at IS NOT NULL
                         THEN roadmap_module_progress.completed_at
                    WHEN LEAST(
                            roadmap_module_progress.total_count,
                            GREATEST(roadmap_module_progress.done_count, EXCLUDED.done_count))
                         = roadmap_module_progress.total_count THEN EXCLUDED.updated_at
                    ELSE NULL
                END,
                updated_at = EXCLUDED.updated_at
            """;

    private final JdbcTemplate jdbcTemplate;

    public void upsertAll(List<ProgressUpdate> updates) {
        if (updates.isEmpty()) return;
        int[] results = jdbcTemplate.batchUpdate(UPSERT_SQL, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                ProgressUpdate update = updates.get(index);
                statement.setLong(1, update.studentId());
                statement.setInt(2, update.roadmapVersion());
                statement.setInt(3, update.weekNumber());
                statement.setInt(4, update.moduleIndex());
                statement.setString(5, update.moduleKey());
                statement.setString(6, update.moduleType());
                if (update.topicId() == null) statement.setNull(7, Types.SMALLINT);
                else statement.setShort(7, update.topicId());
                statement.setInt(8, update.doneCount());
                statement.setInt(9, update.totalCount());
                statement.setString(10, update.status().name());
                if (update.completedAt() == null) statement.setNull(11, Types.TIMESTAMP);
                else statement.setTimestamp(11, Timestamp.valueOf(update.completedAt()));
                statement.setTimestamp(12, Timestamp.valueOf(update.updatedAt()));
            }

            @Override
            public int getBatchSize() {
                return updates.size();
            }
        });
        for (int result : results) {
            if (result == java.sql.Statement.EXECUTE_FAILED) {
                throw new IllegalStateException("Roadmap progress batch upsert failed");
            }
        }
    }

    public record ProgressUpdate(
            long studentId,
            int roadmapVersion,
            int weekNumber,
            int moduleIndex,
            String moduleKey,
            String moduleType,
            Short topicId,
            int doneCount,
            int totalCount,
            RoadmapModuleStatus status,
            LocalDateTime completedAt,
            LocalDateTime updatedAt) {
    }
}
