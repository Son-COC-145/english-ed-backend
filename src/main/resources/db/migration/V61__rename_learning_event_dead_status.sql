ALTER TABLE public.learning_events
    DROP CONSTRAINT ck_learning_event_status;

UPDATE public.learning_events
SET status = 'FAILED'
WHERE status = 'DEAD';

ALTER TABLE public.learning_events
    ADD CONSTRAINT ck_learning_event_status
        CHECK (status IN ('PENDING', 'PROCESSING', 'DONE', 'FAILED'));
