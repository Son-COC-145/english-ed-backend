-- Client-generated UUIDs make answer retries safe across timeouts and reconnects.
ALTER TABLE public.placement_test_answers
    ADD COLUMN IF NOT EXISTS submission_id UUID,
    ADD COLUMN IF NOT EXISTS request_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS submission_type VARCHAR(20);

CREATE UNIQUE INDEX IF NOT EXISTS uk_placement_answer_session_submission
    ON public.placement_test_answers (session_id, submission_id)
    WHERE submission_id IS NOT NULL;

ALTER TABLE public.placement_test_answers
    ADD CONSTRAINT placement_answer_submission_type_check
        CHECK (submission_type IS NULL OR submission_type IN ('ANSWER', 'PRONUNCIATION'));
