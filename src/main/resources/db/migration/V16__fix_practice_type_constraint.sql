-- V16: Mở rộng CHECK constraint trên cột practice_type để cho phép thêm giá trị IPA_PHONEME
-- Lý do: Enum PracticeType trong Java có IPA_PHONEME nhưng constraint DB cũ chưa bao gồm giá trị này.

ALTER TABLE pronunciation_practice_logs
    DROP CONSTRAINT IF EXISTS pronunciation_practice_logs_practice_type_check;

ALTER TABLE pronunciation_practice_logs
    ADD CONSTRAINT pronunciation_practice_logs_practice_type_check
        CHECK (practice_type IN ('PHONEME', 'IPA_PHONEME', 'WORD', 'SENTENCE'));
