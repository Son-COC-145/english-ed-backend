ALTER TABLE teaching_materials
    ADD COLUMN cloudinary_public_id VARCHAR(500),
    ADD COLUMN cloudinary_resource_type VARCHAR(20);

CREATE INDEX idx_teaching_materials_cloudinary_public_id
    ON teaching_materials (cloudinary_public_id)
    WHERE cloudinary_public_id IS NOT NULL;

CREATE TABLE notification_outbox (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    notification_type VARCHAR(50) NOT NULL,
    title VARCHAR(300) NOT NULL,
    body TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP,
    sent_at TIMESTAMP,
    last_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_outbox_dispatch
    ON notification_outbox (status, available_at);

ALTER TABLE notifications
    ADD COLUMN outbox_event_id BIGINT UNIQUE REFERENCES notification_outbox(id);

