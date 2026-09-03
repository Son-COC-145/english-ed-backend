-- ============================================================
-- V23: Add is_placement_skipped flag to student_onboarding table
--
-- Muc tieu:
--   Phan biet ro rang giua COMPLETED (hoan thanh bai test thuc te)
--   va SKIPPED (nguoi dung chu dong bo qua bai test A1).
-- ============================================================

ALTER TABLE student_onboarding
    ADD COLUMN IF NOT EXISTS is_placement_skipped BOOLEAN NOT NULL DEFAULT FALSE;
