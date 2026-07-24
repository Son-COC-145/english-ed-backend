ALTER TABLE student_vocabulary_progress
ADD COLUMN easiness_factor FLOAT DEFAULT 2.5,
ADD COLUMN interval_days INT DEFAULT 0,
ADD COLUMN repetitions INT DEFAULT 0;
