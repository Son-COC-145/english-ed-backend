-- Extend CEFR validation consistently across all CEFR-bearing tables.
ALTER TABLE public.topics DROP CONSTRAINT IF EXISTS topics_cefr_level_check;
ALTER TABLE public.topics ADD CONSTRAINT topics_cefr_level_check
    CHECK (cefr_level IS NULL OR cefr_level IN ('A1','A2','B1','B2','C1','C2'));

ALTER TABLE public.vocabulary DROP CONSTRAINT IF EXISTS vocabulary_cefr_level_check;
ALTER TABLE public.vocabulary ADD CONSTRAINT vocabulary_cefr_level_check
    CHECK (cefr_level IN ('A1','A2','B1','B2','C1','C2'));

ALTER TABLE public.questions DROP CONSTRAINT IF EXISTS questions_cefr_level_check;
ALTER TABLE public.questions ADD CONSTRAINT questions_cefr_level_check
    CHECK (cefr_level IN ('A1','A2','B1','B2','C1','C2'));

ALTER TABLE public.speaking_scenarios DROP CONSTRAINT IF EXISTS speaking_scenarios_cefr_level_check;
ALTER TABLE public.speaking_scenarios ADD CONSTRAINT speaking_scenarios_cefr_level_check
    CHECK (cefr_level IN ('A1','A2','B1','B2','C1','C2'));

ALTER TABLE public.student_onboarding DROP CONSTRAINT IF EXISTS student_onboarding_placement_cefr_level_check;
ALTER TABLE public.student_onboarding ADD CONSTRAINT student_onboarding_placement_cefr_level_check
    CHECK (placement_cefr_level IS NULL OR placement_cefr_level IN ('A1','A2','B1','B2','C1','C2'));
