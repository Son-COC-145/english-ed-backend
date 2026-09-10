-- V31: Thêm bảng idempotency_keys để chống duplicate submit cho Mini-game và Review
CREATE TABLE idempotency_keys (
    id          BIGSERIAL    PRIMARY KEY,
    attempt_id  VARCHAR(36)  NOT NULL,
    user_id     BIGINT       NOT NULL,
    result_json JSONB,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_attempt UNIQUE (user_id, attempt_id)
);

-- Index để cleanup job chạy hiệu quả
CREATE INDEX idx_idempotency_created_at ON idempotency_keys(created_at);
