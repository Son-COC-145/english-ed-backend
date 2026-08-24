-- ============================================================
-- V22: Placement Test - DB-level Constraints & Performance Indexes
--
-- Muc tieu:
--   1. Enforce "1 active session per user" tai DB de chan race condition
--   2. Enforce answer idempotency: (session_id, question_id) UNIQUE
--   3. Them covering index cho cac hot query path
-- ============================================================

-- 1. Partial unique index: moi user chi duoc co 1 session dang lam
CREATE UNIQUE INDEX IF NOT EXISTS uidx_placement_sessions_one_active_per_student
    ON placement_test_sessions (student_id)
    WHERE is_completed = false;

-- 2. Composite index: lookup session cua user (findTop...Desc)
CREATE INDEX IF NOT EXISTS idx_placement_sessions_student_started
    ON placement_test_sessions (student_id, started_at DESC);

-- 3. Index tren is_completed de filter session status nhanh
CREATE INDEX IF NOT EXISTS idx_placement_sessions_completed
    ON placement_test_sessions (is_completed, student_id)
    WHERE is_completed = false;

-- 4. UNIQUE constraint: (session_id, question_id) - answer idempotency
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_placement_answers_session_question'
    ) THEN
        ALTER TABLE placement_test_answers
            ADD CONSTRAINT uk_placement_answers_session_question
            UNIQUE (session_id, question_id);
    END IF;
END $$;

-- 5. Covering index: session_id + answered_at cho query lay danh sach
CREATE INDEX IF NOT EXISTS idx_placement_answers_session_answered
    ON placement_test_answers (session_id, answered_at ASC);

-- 6. Index cho question picking: cefr_level + is_active (partial)
CREATE INDEX IF NOT EXISTS idx_questions_active_by_level
    ON questions (cefr_level, id)
    WHERE is_active = true;
