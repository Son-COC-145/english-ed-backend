-- V46: Fix placement_test_sessions missing C2 in CEFR check constraint
--
-- Root cause: V1__init.sql tạo constraint chỉ có A1-C1, bỏ sót C2.
-- V38__add_c2_cefr_support.sql đã fix các table khác (topics, vocabulary,
-- questions, speaking_scenarios, student_onboarding) nhưng QUÊN bảng này.
--
-- Symptom: "violates check constraint placement_test_sessions_current_cefr_estimate_check"
-- khi CAT algorithm assign C2 cho user đạt trình độ cao (câu 11+, score > 80%).
-- Failing row: (confidence=73.33, currentCefrEstimate=C2, questionIndex=11)

ALTER TABLE public.placement_test_sessions
    DROP CONSTRAINT IF EXISTS placement_test_sessions_current_cefr_estimate_check;

ALTER TABLE public.placement_test_sessions
    ADD CONSTRAINT placement_test_sessions_current_cefr_estimate_check
        CHECK (current_cefr_estimate IS NULL
            OR current_cefr_estimate IN ('A1','A2','B1','B2','C1','C2'));
