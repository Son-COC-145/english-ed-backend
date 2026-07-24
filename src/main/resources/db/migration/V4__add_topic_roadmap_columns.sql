ALTER TABLE public.topics ADD COLUMN IF NOT EXISTS cefr_level VARCHAR(10);
ALTER TABLE public.topics ADD COLUMN IF NOT EXISTS category  VARCHAR(50);

ALTER TABLE public.topics ADD CONSTRAINT topics_cefr_level_check
    CHECK (cefr_level IS NULL OR cefr_level IN ('A1','A2','B1','B2','C1'));

ALTER TABLE public.topics ADD CONSTRAINT topics_category_check
    CHECK (category IS NULL OR category IN (
        'WORK','TRAVEL','EXAM_IELTS','DAILY_CONVERSATION','STUDY_ABROAD'
    ));

CREATE INDEX IF NOT EXISTS idx_topics_cefr_category
    ON public.topics (cefr_level, category)
    WHERE is_active = true;
