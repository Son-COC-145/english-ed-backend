-- Keep the Java/API durationSeconds Integer type aligned with PostgreSQL.
ALTER TABLE minigame_results
    ALTER COLUMN duration_seconds TYPE integer
    USING duration_seconds::integer;
