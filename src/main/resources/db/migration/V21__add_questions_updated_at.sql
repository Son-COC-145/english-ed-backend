-- V21: Thêm cột updated_at vào bảng questions để Admin biết câu hỏi được sửa lần cuối khi nào.
-- Dùng IF NOT EXISTS để idempotent – chạy lại không lỗi.

ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;

-- Khởi tạo giá trị cho các row cũ = created_at (thời điểm tạo)
UPDATE questions
SET updated_at = created_at
WHERE updated_at IS NULL;
