-- V9__add_failed_jobs_table.sql
-- Bảng Dead Letter Queue (DLQ) cho cơ chế Fault Tolerance 2 lớp
-- Lưu các event cộng XP thất bại sau 3 lần retry để Cron Job xử lý lại

CREATE TABLE IF NOT EXISTS failed_jobs (
    id              BIGSERIAL PRIMARY KEY,
    job_type        VARCHAR(100)    NOT NULL DEFAULT 'PRONUNCIATION_XP',
    student_id      BIGINT          NOT NULL,
    payload_json    JSONB           NOT NULL,           -- Toàn bộ event object lưu dạng JSON
    error_message   TEXT,                               -- Lỗi cuối cùng trước khi ghi DLQ
    retry_count     SMALLINT        NOT NULL DEFAULT 0, -- Số lần đã retry
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING', -- PENDING | PROCESSED | DEAD
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMP                           -- Thời điểm Cron Job xử lý thành công
);

CREATE INDEX idx_failed_jobs_status_created ON failed_jobs (status, created_at);
