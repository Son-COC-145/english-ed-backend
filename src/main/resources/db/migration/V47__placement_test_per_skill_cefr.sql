-- V47: Placement Test Per-Skill CEFR Estimates (Phase 1 Redesign)
--
-- Thêm 5 cột per-skill adaptive CEFR estimate vào placement_test_sessions
-- (trạng thái trong session) và 5 cột kết quả vào student_onboarding
-- (persisted sau khi hoàn thành test).
--
-- current_cefr_estimate cũ KHÔNG bị DROP — giữ nguyên để tránh mất dữ liệu.
-- Code mới sẽ không write vào cột đó nữa.

-- ── placement_test_sessions: Per-skill adaptive state ──────────────────────

ALTER TABLE public.placement_test_sessions
    ADD COLUMN IF NOT EXISTS vocab_cefr_estimate         VARCHAR(10),
    ADD COLUMN IF NOT EXISTS grammar_cefr_estimate       VARCHAR(10),
    ADD COLUMN IF NOT EXISTS reading_cefr_estimate       VARCHAR(10),
    ADD COLUMN IF NOT EXISTS listening_cefr_estimate     VARCHAR(10),
    ADD COLUMN IF NOT EXISTS pronunciation_cefr_estimate VARCHAR(10);

ALTER TABLE public.placement_test_sessions
    ADD CONSTRAINT IF NOT EXISTS pts_vocab_cefr_check
        CHECK (vocab_cefr_estimate IS NULL
            OR vocab_cefr_estimate IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS pts_grammar_cefr_check
        CHECK (grammar_cefr_estimate IS NULL
            OR grammar_cefr_estimate IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS pts_reading_cefr_check
        CHECK (reading_cefr_estimate IS NULL
            OR reading_cefr_estimate IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS pts_listening_cefr_check
        CHECK (listening_cefr_estimate IS NULL
            OR listening_cefr_estimate IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS pts_pronunciation_cefr_check
        CHECK (pronunciation_cefr_estimate IS NULL
            OR pronunciation_cefr_estimate IN ('A1','A2','B1','B2','C1','C2'));

-- ── student_onboarding: Per-skill CEFR result (persisted) ─────────────────
-- NOTE: placement_vocab_score ... pronunciation_score đã tồn tại (V1__init.sql).
-- Chỉ thêm 5 cột CEFR per-skill MỚI (scores là field cũ, không thêm lại).

ALTER TABLE public.student_onboarding
    ADD COLUMN IF NOT EXISTS placement_vocab_cefr         VARCHAR(10),
    ADD COLUMN IF NOT EXISTS placement_grammar_cefr       VARCHAR(10),
    ADD COLUMN IF NOT EXISTS placement_reading_cefr       VARCHAR(10),
    ADD COLUMN IF NOT EXISTS placement_listening_cefr     VARCHAR(10),
    ADD COLUMN IF NOT EXISTS placement_pronunciation_cefr VARCHAR(10);

ALTER TABLE public.student_onboarding
    ADD CONSTRAINT IF NOT EXISTS so_vocab_cefr_check
        CHECK (placement_vocab_cefr IS NULL
            OR placement_vocab_cefr IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS so_grammar_cefr_check
        CHECK (placement_grammar_cefr IS NULL
            OR placement_grammar_cefr IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS so_reading_cefr_check
        CHECK (placement_reading_cefr IS NULL
            OR placement_reading_cefr IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS so_listening_cefr_check
        CHECK (placement_listening_cefr IS NULL
            OR placement_listening_cefr IN ('A1','A2','B1','B2','C1','C2')),
    ADD CONSTRAINT IF NOT EXISTS so_pronunciation_cefr_check
        CHECK (placement_pronunciation_cefr IS NULL
            OR placement_pronunciation_cefr IN ('A1','A2','B1','B2','C1','C2'));
