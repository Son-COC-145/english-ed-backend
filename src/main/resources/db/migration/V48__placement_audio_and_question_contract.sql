-- Placement audio is pre-generated and stored separately from learner-facing question content.
ALTER TABLE public.questions
    ADD COLUMN IF NOT EXISTS placement_audio_url VARCHAR(1000);

-- Persist the question issued to a session so refresh/retry always returns the same item and a
-- client cannot submit an arbitrary question id.
ALTER TABLE public.placement_test_sessions
    ADD COLUMN IF NOT EXISTS current_question_id BIGINT;

ALTER TABLE public.placement_test_sessions
    ADD CONSTRAINT fk_pts_current_question
        FOREIGN KEY (current_question_id) REFERENCES public.questions(id);

-- Preserve an already-opaque/static URL if one exists. Legacy dynamic TTS URLs deliberately do
-- not qualify and must be generated through the admin media endpoint before the question is used.
UPDATE public.questions
SET placement_audio_url = COALESCE(content_json->>'audioUrl', content_json->>'audio_url')
WHERE skill = 'LISTENING'
  AND placement_audio_url IS NULL
  AND COALESCE(content_json->>'audioUrl', content_json->>'audio_url') IS NOT NULL
  AND COALESCE(content_json->>'audioUrl', content_json->>'audio_url') NOT LIKE '%/tts/stream%'
  AND COALESCE(content_json->>'audioUrl', content_json->>'audio_url') NOT LIKE '%example.com%';

-- Canonicalize the internal JSON contract. Listening transcript remains internal and is removed
-- by the learner response mapper; its audio URL lives in the dedicated column above.
UPDATE public.questions
SET content_json = (content_json - 'text')
                   || jsonb_build_object('passage', content_json->'text')
WHERE question_type = 'READING_COMPREHENSION'
  AND content_json ? 'text'
  AND NOT content_json ? 'passage';

UPDATE public.questions
SET content_json = (content_json - 'ipa')
                   || jsonb_build_object('ipaTranscription', content_json->'ipa')
WHERE question_type = 'PRONUNCIATION'
  AND content_json ? 'ipa'
  AND NOT content_json ? 'ipaTranscription';

UPDATE public.questions
SET content_json = content_json - 'audio_url' - 'audioUrl'
WHERE question_type = 'LISTENING';

CREATE INDEX IF NOT EXISTS idx_questions_ready_listening
    ON public.questions (cefr_level, id)
    WHERE is_active = true
      AND skill = 'LISTENING'
      AND placement_audio_url IS NOT NULL;

-- Store only derived scoring metadata for pronunciation; raw audio is never persisted.
ALTER TABLE public.placement_test_answers
    ADD COLUMN IF NOT EXISTS pronunciation_overall_score      SMALLINT,
    ADD COLUMN IF NOT EXISTS pronunciation_accuracy_score     SMALLINT,
    ADD COLUMN IF NOT EXISTS pronunciation_fluency_score      SMALLINT,
    ADD COLUMN IF NOT EXISTS pronunciation_completeness_score SMALLINT;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'placement_test_sessions'
          AND column_name = 'confidence_score'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'placement_test_sessions'
          AND column_name = 'progress_percent'
    ) THEN
        ALTER TABLE public.placement_test_sessions
            RENAME COLUMN confidence_score TO progress_percent;
    END IF;
END $$;
