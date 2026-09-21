-- Scope idempotency by operation and retain a fingerprint to reject key reuse with another payload.
ALTER TABLE idempotency_keys
    ADD COLUMN IF NOT EXISTS operation_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64);

UPDATE idempotency_keys
SET operation_type = COALESCE(operation_type, 'LEGACY'),
    request_hash = COALESCE(request_hash, 'LEGACY');

ALTER TABLE idempotency_keys
    ALTER COLUMN operation_type SET NOT NULL,
    ALTER COLUMN request_hash SET NOT NULL;

ALTER TABLE idempotency_keys
    DROP CONSTRAINT IF EXISTS uq_user_attempt;

ALTER TABLE idempotency_keys
    DROP CONSTRAINT IF EXISTS uq_user_operation_attempt;

ALTER TABLE idempotency_keys
    ADD CONSTRAINT uq_user_operation_attempt UNIQUE (user_id, operation_type, attempt_id);
