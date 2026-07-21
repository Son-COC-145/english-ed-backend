# PHASE 1: SURVEY & SETTINGS

Phần này bao gồm Bước 1, Bước 2 và Bước 6 của luồng Onboarding. Các bước này chủ yếu xử lý việc lấy thông tin người dùng và cài đặt ban đầu.

## 1. Màn hình Welcome (Bước 1)
- **Mục tiêu:** Chào mừng người dùng, hiển thị tên và avatar AI.
- **Yêu cầu kỹ thuật:**
  - API `GET /api/v1/onboarding/status`: Kiểm tra người dùng đã hoàn thành onboarding chưa. Nếu rồi, redirect thẳng sang Dashboard.
  - Phản hồi $\le$ 2 giây. Hỗ trợ i18n (Tiếng Việt/Anh).

## 2. Goal Survey (Bước 2)
- **Mục tiêu:** Lấy 5 thông tin: Mục tiêu học, Kỹ năng cần cải thiện, Thời gian học mỗi ngày, Môi trường học thích, Trình độ nền tảng.
- **Lưu trữ Database:**
  - Bảng `student_onboarding`.
  - Lưu dạng `JSONB` trong cột `goal_survey_json`. Định dạng JSON linh hoạt cho phép thay đổi câu hỏi ở Frontend mà Backend không cần migration DB.
- **API Contract:**
  - `POST /api/v1/onboarding/goal-survey`
  - Body: Danh sách câu trả lời. Có thể chọn nhiều đáp án (ví dụ câu hỏi số 2).

## 3. Daily Goal & Notification (Bước 6)
- **Mục tiêu:** Chốt mức độ XP luyện tập (10/20/30/50) và khung giờ nhắc nhở.
- **Cơ chế Streak:**
  - `StudentStat` sẽ quản lý `current_streak` và `longest_streak`.
  - Mất 1 ngày không hoàn thành XP mục tiêu sẽ reset streak về 0.
- **API Contract:**
  - `POST /api/v1/onboarding/settings`
  - Body: `daily_goal_xp` (Integer), `reminder_time` (String HH:mm).
