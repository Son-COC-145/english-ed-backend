-- Durable account-erasure workflow. The user row is retained as an anonymized
-- tombstone so billing records can keep their referential integrity.
ALTER TABLE public.users
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

CREATE TABLE public.account_deletion_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES public.users(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    source VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP,
    claim_token VARCHAR(36),
    last_error VARCHAR(300),
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    CONSTRAINT uk_account_deletion_user UNIQUE (user_id),
    CONSTRAINT chk_account_deletion_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_account_deletion_source
        CHECK (source IN ('IN_APP', 'WEB'))
);

CREATE INDEX idx_account_deletion_dispatch
    ON public.account_deletion_requests(status, available_at);
