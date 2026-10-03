package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.entity.enums.RoadmapModuleStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapProgressBatchWriterTest {

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private PreparedStatement statement;

    @Test
    void writesAllProgressRowsInOneJdbcBatch() throws Exception {
        RoadmapProgressBatchWriter writer = new RoadmapProgressBatchWriter(jdbcTemplate);
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 12, 0);
        RoadmapProgressBatchWriter.ProgressUpdate update =
                new RoadmapProgressBatchWriter.ProgressUpdate(
                        7L, 3, 2, 1, "VOCABULARY:12",
                        "VOCABULARY", (short) 12, 5, 10,
                        RoadmapModuleStatus.IN_PROGRESS, null, now);
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{1});

        writer.upsertAll(List.of(update));

        ArgumentCaptor<BatchPreparedStatementSetter> captor =
                ArgumentCaptor.forClass(BatchPreparedStatementSetter.class);
        verify(jdbcTemplate).batchUpdate(anyString(), captor.capture());
        assertThat(captor.getValue().getBatchSize()).isEqualTo(1);
        captor.getValue().setValues(statement, 0);
        verify(statement).setLong(1, 7L);
        verify(statement).setInt(4, 1);
        verify(statement).setString(5, update.moduleKey());
        verify(statement).setInt(8, 5);
        verify(statement).setString(10, "IN_PROGRESS");
    }

    @Test
    void failsTransactionWhenDriverReportsFailedBatchEntry() {
        RoadmapProgressBatchWriter writer = new RoadmapProgressBatchWriter(jdbcTemplate);
        LocalDateTime now = LocalDateTime.of(2026, 10, 3, 12, 0);
        RoadmapProgressBatchWriter.ProgressUpdate update =
                new RoadmapProgressBatchWriter.ProgressUpdate(
                        7L, 3, 1, 0, "VOCABULARY:1", "VOCABULARY",
                        null, 0, 1, RoadmapModuleStatus.NOT_STARTED, null, now);
        when(jdbcTemplate.batchUpdate(anyString(), any(BatchPreparedStatementSetter.class)))
                .thenReturn(new int[]{Statement.EXECUTE_FAILED});

        assertThatThrownBy(() -> writer.upsertAll(List.of(update)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("batch upsert failed");
    }
}
