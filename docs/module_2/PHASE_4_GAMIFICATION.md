 # GIAI ĐOẠN 4: GAMIFICATION VÀ MINIGAMES ENGINE

## 1. Mục tiêu (Objective)
Xử lý logic khi học viên hoàn thành xong 1 lượt chơi game (Listen-Choose, Scramble...). Tính toán điểm XP, cập nhật streak học tập, và đổi trạng thái Flashcard để phục vụ ôn tập (Spaced Repetition).

## 2. Chi tiết thực hiện (Implementation Steps)

### Bước 1: Xây dựng Gamification Service
- **Logic tính XP (XP Calculation):**
  - XP Cơ bản = Phụ thuộc vào `GameType` (VD: Nghe Chọn Từ = 10 XP, Điền Từ = 20 XP).
  - Bonus Time = Càng làm nhanh điểm càng cao (Dựa vào `duration_seconds` client gửi lên).
  - Bonus Streak = Nếu chuỗi thắng liên tiếp lớn hơn N, x1.5 hoặc x2 XP.
- **Xử lý `StudentStat` (Bảng thống kê chung):**
  - Lấy bản ghi `StudentStat` của User.
  - Cộng `total_xp`.
  - Cập nhật `last_activity_date`. Nếu ngày chơi là ngày hôm nay -> Giữ nguyên streak. Nếu là ngày tiếp theo -> `current_streak += 1`. Nếu cách 2 ngày trở lên -> `current_streak = 0`.
  - So sánh và cập nhật `longest_streak`.

### Bước 2: Xử lý Tracking Từ Vựng (Vocabulary Progress)
Mỗi từ vựng sẽ được track riêng thông qua `StudentVocabularyProgress`.
- **Nếu Client báo làm Đúng (isCorrect = true):**
  - `correct_count += 1`.
  - Nếu `correct_count` đạt một ngưỡng nhất định (Ví dụ >= 3 lần), tự động đổi `status` từ `NEW` (Chưa học) hoặc `LEARNING` thành `MASTERED` (Đã thuộc).
- **Nếu Client báo làm Sai (isCorrect = false):**
  - `incorrect_count += 1`.
  - Đổi `status` thành `REVIEWING` (Cần ôn tập gấp).

### Bước 3: Xây dựng API (MinigameController.java)
- API: `POST /api/v1/vocabularies/minigames/submit`
- **Request Body (DTO):**
```json
{
  "vocabularyId": 123,
  "gameType": "LISTEN_CHOOSE",
  "isCorrect": true,
  "durationSeconds": 5
}
```
- Gọi Service thực thi các logic ở Bước 1 & Bước 2. Cuối cùng Insert 1 dòng log vào bảng `minigame_results`.
- **Response:** Trả về số lượng XP nhận được, trạng thái Streak hiện tại, và trạng thái từ vựng để Frontend có màn hình chúc mừng (Congratulation Screen) nổ pháo hoa.
