-- Audio analysis metadata and idempotent speaking rewards.
-- V34 was shipped with a few client-encoding-corrupted Vietnamese labels. Keep
-- those rows readable until a localized catalog is supplied by content editors.
UPDATE speaking_scenarios SET title_vi = title_en WHERE title_vi LIKE '%?%';
ALTER TABLE speaking_turns ADD COLUMN IF NOT EXISTS recorded_at timestamp;
ALTER TABLE speaking_turns ADD COLUMN IF NOT EXISTS duration_seconds double precision;
ALTER TABLE speaking_turns ADD COLUMN IF NOT EXISTS audio_analysis_status varchar(32) NOT NULL DEFAULT 'PENDING';
ALTER TABLE speaking_turns ADD COLUMN IF NOT EXISTS metrics_version varchar(32);
ALTER TABLE speaking_turns ADD COLUMN IF NOT EXISTS audio_status varchar(32) NOT NULL DEFAULT 'PENDING';
ALTER TABLE speaking_turns ADD COLUMN IF NOT EXISTS audio_error_code varchar(100);

CREATE INDEX IF NOT EXISTS speaking_scenario_cefr_active
    ON speaking_scenarios(cefr_level, is_active);

CREATE TABLE IF NOT EXISTS speaking_reward_ledger (
    session_id bigint PRIMARY KEY REFERENCES speaking_sessions(id),
    student_id bigint NOT NULL REFERENCES users(id),
    xp_amount smallint NOT NULL,
    created_at timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
);
