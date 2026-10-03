CREATE TABLE public.learning_events (
    event_id UUID PRIMARY KEY,
    seq BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE,
    student_id BIGINT NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    event_type VARCHAR(50) NOT NULL,
    source VARCHAR(40) NOT NULL,
    source_reference VARCHAR(120) NOT NULL,
    skill VARCHAR(20),
    entity_type VARCHAR(30),
    entity_id BIGINT,
    score SMALLINT,
    duration_seconds INTEGER,
    duration_source VARCHAR(10),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    schema_version SMALLINT NOT NULL DEFAULT 1,
    occurred_at TIMESTAMP NOT NULL,
    causation_event_id UUID,
    status VARCHAR(12) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error TEXT,
    correlation_id VARCHAR(64),
    processing_started_at TIMESTAMP,
    processed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_learning_event_source
        UNIQUE (student_id, source, source_reference, event_type),
    CONSTRAINT ck_learning_event_score
        CHECK (score IS NULL OR score BETWEEN 0 AND 100),
    CONSTRAINT ck_learning_event_duration
        CHECK (duration_seconds IS NULL OR duration_seconds >= 0),
    CONSTRAINT ck_learning_event_duration_source
        CHECK (duration_source IS NULL OR duration_source IN ('MEASURED', 'ESTIMATED')),
    CONSTRAINT ck_learning_event_schema_version
        CHECK (schema_version > 0),
    CONSTRAINT ck_learning_event_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'DEAD')),
    CONSTRAINT ck_learning_event_attempt_count
        CHECK (attempt_count >= 0)
);

CREATE INDEX idx_learning_events_dispatch
    ON public.learning_events (status, next_retry_at, seq);

CREATE INDEX idx_learning_events_student_sequence
    ON public.learning_events (student_id, seq);

