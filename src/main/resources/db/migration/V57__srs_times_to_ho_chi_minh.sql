-- The app now computes every "today"/due time in Asia/Ho_Chi_Minh (UTC+7, no DST).
-- Until now the SRS clock (GameficationService) wrote these columns in UTC wall-clock time,
-- so shift the stored values by +7h to keep each review due at the same real moment.
UPDATE student_vocabulary_progress
SET next_review_at    = next_review_at + INTERVAL '7 hours',
    last_practiced_at = last_practiced_at + INTERVAL '7 hours'
WHERE next_review_at IS NOT NULL
   OR last_practiced_at IS NOT NULL;

-- Idempotency keys were also stamped with the UTC clock; the 24h cleanup job compares them with local time.
UPDATE idempotency_keys
SET created_at = created_at + INTERVAL '7 hours';
