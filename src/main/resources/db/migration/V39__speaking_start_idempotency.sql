CREATE TABLE IF NOT EXISTS speaking_start_requests (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES users(id),
    request_key VARCHAR(100) NOT NULL,
    scenario_id SMALLINT NOT NULL REFERENCES speaking_scenarios(id),
    session_id BIGINT REFERENCES speaking_sessions(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_speaking_start_request UNIQUE (student_id, request_key)
);

CREATE INDEX IF NOT EXISTS idx_speaking_start_requests_session
    ON speaking_start_requests(session_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_speaking_start_request_session
    ON speaking_start_requests(session_id)
    WHERE session_id IS NOT NULL;
