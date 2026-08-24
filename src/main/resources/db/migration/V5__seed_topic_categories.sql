UPDATE public.topics SET cefr_level = 'A1', category = 'DAILY_CONVERSATION'
    WHERE name_en ILIKE '%greeting%' OR name_en ILIKE '%family%' OR name_en ILIKE '%daily%';

UPDATE public.topics SET cefr_level = 'A2', category = 'TRAVEL'
    WHERE name_en ILIKE '%travel%' OR name_en ILIKE '%hotel%' OR name_en ILIKE '%airport%';

UPDATE public.topics SET cefr_level = 'B1', category = 'WORK'
    WHERE name_en ILIKE '%business%' OR name_en ILIKE '%office%' OR name_en ILIKE '%work%';

UPDATE public.topics SET cefr_level = 'B2', category = 'EXAM_IELTS'
    WHERE name_en ILIKE '%ielts%' OR name_en ILIKE '%exam%';

UPDATE public.topics SET cefr_level = 'C1', category = 'STUDY_ABROAD'
    WHERE name_en ILIKE '%abroad%' OR name_en ILIKE '%academic%';

-- Fallback for any other topics
UPDATE public.topics SET cefr_level = 'A1', category = 'DAILY_CONVERSATION'
    WHERE cefr_level IS NULL OR category IS NULL;
