-- NULL means the learner has not submitted the settings step yet. A hard-coded default of 20
-- incorrectly made new onboarding records appear to have completed that step.
ALTER TABLE public.student_onboarding
    ALTER COLUMN daily_goal_xp DROP NOT NULL;
