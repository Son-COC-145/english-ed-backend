ALTER TABLE public.student_onboarding
    ADD COLUMN IF NOT EXISTS roadmap_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS roadmap_generation_version INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS roadmap_generation_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS roadmap_last_error VARCHAR(200),
    ADD COLUMN IF NOT EXISTS roadmap_updated_at TIMESTAMP;

ALTER TABLE public.student_onboarding
    ADD CONSTRAINT student_onboarding_roadmap_status_check
        CHECK (roadmap_status IS NULL OR roadmap_status IN ('PENDING','PROCESSING','READY','FAILED'));

UPDATE public.student_onboarding
SET roadmap_status = 'READY',
    roadmap_updated_at = COALESCE(placement_completed_at, CURRENT_TIMESTAMP)
WHERE roadmap_json IS NOT NULL;

UPDATE public.student_onboarding
SET roadmap_status = 'PENDING',
    roadmap_generation_version = 1,
    roadmap_updated_at = CURRENT_TIMESTAMP
WHERE placement_cefr_level IS NOT NULL
  AND roadmap_json IS NULL
  AND roadmap_status IS NULL;

CREATE TABLE public.roadmap_generation_jobs (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES public.users(id),
    generation_version INTEGER NOT NULL,
    cefr_level VARCHAR(10) NOT NULL,
    goal_survey_json JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    locked_at TIMESTAMP,
    claim_token VARCHAR(36),
    last_error VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT roadmap_job_status_check
        CHECK (status IN ('PENDING','PROCESSING','READY','FAILED')),
    CONSTRAINT roadmap_job_cefr_check
        CHECK (cefr_level IN ('A1','A2','B1','B2','C1','C2')),
    CONSTRAINT uk_roadmap_job_student_version UNIQUE (student_id, generation_version)
);

INSERT INTO public.roadmap_generation_jobs (
    student_id, generation_version, cefr_level, goal_survey_json, status, available_at)
SELECT student_id,
       roadmap_generation_version,
       placement_cefr_level,
       goal_survey_json,
       'PENDING',
       CURRENT_TIMESTAMP
FROM public.student_onboarding
WHERE roadmap_status = 'PENDING'
ON CONFLICT (student_id, generation_version) DO NOTHING;

CREATE INDEX idx_roadmap_jobs_dispatch
    ON public.roadmap_generation_jobs (status, available_at);
