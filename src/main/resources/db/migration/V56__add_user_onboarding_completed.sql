ALTER TABLE public.users
    ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE public.users AS users
SET onboarding_completed = TRUE
FROM public.student_onboarding AS onboarding
WHERE onboarding.student_id = users.id
  AND onboarding.onboarding_completed = TRUE;
