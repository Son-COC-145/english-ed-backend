INSERT INTO users (email, password_hash, full_name, role, provider, locale, is_active, created_at, updated_at) VALUES
('admin@example.com', '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'System Admin', 'ADMIN', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('teacher@example.com', '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'Demo Teacher', 'TEACHER', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('student@example.com', '$2a$10$G/fVEytO0zpyzNRzf/jf8uG1eHwDS5JJp1Ly9TJTgzasjP7AQsvQ.', 'Demo Student', 'STUDENT', 'LOCAL', 'vi', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
