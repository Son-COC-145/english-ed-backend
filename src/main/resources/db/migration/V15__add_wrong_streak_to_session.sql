-- =============================================================================
-- V15: Thêm cột current_wrong_streak vào placement_test_sessions
--
-- Mục đích: Lưu trực tiếp số lần trả lời sai liên tiếp vào session thay vì
-- phải load toàn bộ bảng placement_test_answers để đếm lại mỗi lần
-- submitAnswer() được gọi (N+1 query).
--
-- Giá trị mặc định = 0 để tương thích với các session đang tồn tại.
-- =============================================================================

ALTER TABLE placement_test_sessions
    ADD COLUMN IF NOT EXISTS current_wrong_streak INTEGER NOT NULL DEFAULT 0;

COMMENT ON COLUMN placement_test_sessions.current_wrong_streak
    IS 'Số lần trả lời sai liên tiếp. Reset = 0 khi đúng, tăng +1 khi sai. Tránh N+1 query khi tính confidence score.';
