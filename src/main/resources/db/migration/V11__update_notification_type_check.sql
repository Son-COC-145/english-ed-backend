ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check CHECK (type IN ('STREAK_REMINDER', 'ASSIGNMENT_DUE', 'NEW_MATERIAL', 'TEACHER_COMMENT', 'SYSTEM', 'ASSIGNMENT', 'GRADE'));
