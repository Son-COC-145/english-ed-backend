-- Short-lived distributed claim: concurrent retries must not all call the paid speech provider.
-- No learner audio is stored; only a non-reversible request hash and coordination metadata.
CREATE TABLE public.placement_pronunciation_submissions (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES public.placement_test_sessions(id) ON DELETE CASCADE,
    question_id BIGINT NOT NULL REFERENCES public.questions(id),
    submission_id UUID NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pronunciation_submission_status_check
        CHECK (status IN ('PROCESSING', 'COMPLETED')),
    CONSTRAINT uk_pronunciation_submission_key UNIQUE (session_id, submission_id),
    CONSTRAINT uk_pronunciation_submission_question UNIQUE (session_id, question_id)
);

CREATE INDEX idx_pronunciation_submission_stale
    ON public.placement_pronunciation_submissions (status, updated_at);
