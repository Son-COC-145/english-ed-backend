# Module 5 – Adaptive Learning & Progress Intelligence

> Người tạo: Nguyễn Đức Mạnh · Được tạo bởi: Claude Opus 5.5 (AI) · Ngày tạo: 29/09/2026 · Cập nhật: 29/09/2026
> Trạng thái: Bản nháp, chờ nhóm chốt · Nhánh tham chiếu: `main` · Migration mới nhất: `V57`

**Mục tiêu:** Backend biết học viên đang ở trình độ nào, tiến bộ ra sao và nên học gì tiếp theo, rồi trả kết quả đã tính sẵn cho Mobile. Các module hiện có vẫn là nguồn dữ liệu gốc. Module 5 chỉ tổng hợp dữ liệu từ các module đó.

---

## 1. Hiện trạng

| Module | Đang có | Dùng được cho Module 5 |
|---|---|---|
| Placement | Lưu CEFR và điểm 0–100 cho 5 kỹ năng: Vocab, Grammar, Reading, Listening, Pronunciation | Tạo hồ sơ năng lực ban đầu |
| Goal survey | Mục tiêu học, kỹ năng muốn tập trung (chữ tự do), số phút học mỗi ngày (tuỳ chọn) | Ưu tiên đề xuất theo mục tiêu |
| Từ vựng (SRS) | Lịch ôn từng từ theo SM-2, trạng thái NEW → MASTERED | Gợi ý "từ đến hạn ôn" |
| Mini-game | Lượt chơi theo chủ đề: điểm 0–100, thời lượng | Đo năng lực từ vựng, thời gian học |
| Phát âm | Log từng lần luyện; tính được mức thành thạo từng âm | Gợi ý "âm yếu" |
| Speaking | Phiên hội thoại có điểm hoàn thành nhiệm vụ, lưu loát, ngữ điệu | Đo năng lực nói, thời gian học |
| Bài tập | Học viên xem được bài chưa làm, bài đã nộp và điểm | Gợi ý "bài sắp hết hạn" |
| Gamification | XP, streak, Daily Mission (gợi ý từ vựng theo lịch lớp) | Mốc streak |
| Hạ tầng | Redis, xử lý nền theo lịch, bảng hàng đợi dùng DB, Spring AI, Azure TTS/Blob, Actuator | Xử lý sự kiện, cache, sinh nội dung |

---

## 2. Điểm còn thiếu

1. **Reading, Listening, Grammar chưa có bài luyện.** Ba kỹ năng này chỉ xuất hiện trong placement. Câu hỏi Reading/Listening trong ngân hàng mới là dữ liệu mẫu (~30 câu mỗi loại). Placement cũng không đo Speaking.
   → Điểm của 3 kỹ năng này không bao giờ thay đổi, và không có gì để đề xuất cho chúng.
2. **Roadmap chưa theo dõi tiến độ.** API tiến độ luôn trả "tuần 1, chưa xong module nào". Chưa có chỗ lưu module đã hoàn thành.
   → Chưa biết học viên đang ở tuần/module nào.
3. **Chưa ghi thời gian học.** Cột tổng số phút học có nhưng không được ghi. Luyện phát âm và ôn từ không lưu thời lượng.
   → Chưa trả lời được "hôm nay học bao lâu".
4. **Streak chưa tính cho luyện phát âm.** Mini-game, ôn từ và Speaking đã tính streak. Luyện phát âm chỉ cộng XP; mục tiêu XP hằng ngày chưa được cập nhật.
   → Người chỉ luyện phát âm vẫn bị mất streak, mục tiêu ngày luôn hiện chưa đạt.
5. **Có hai "kế hoạch hôm nay".** Daily Mission đã gợi ý từ vựng. Nếu thêm Today Plan mà không gộp, app sẽ có hai danh sách mâu thuẫn nhau.

---

## 3. Câu hỏi mở

### 3.1 Cần chốt trước khi làm (kèm đề xuất)

| # | Câu hỏi | Đề xuất |
|---|---|---|
| Q1 | Reading/Listening/Grammar có hiển thị trong hồ sơ năng lực khi chưa có bài luyện không? | Có. Gắn nhãn "Theo placement", không đưa vào gợi ý cho tới khi có bài luyện. |
| Q2 | Điểm giáo viên chấm có tính vào năng lực không? | Giai đoạn đầu: không, bài tập chỉ để nhắc hạn nộp. Sau này: điểm giáo viên thay cho điểm tự động của cùng bài làm, không cộng thêm. |
| Q3 | Today Plan và Daily Mission xử lý thế nào? | Today Plan thay thế. Daily Mission giữ tạm cho app bản cũ rồi bỏ. |
| Q4 | Thế nào là "hoàn thành một module roadmap"? | Tự tính từ kết quả học, không có nút đánh dấu. Ví dụ: module từ vựng xong khi chơi một lượt của chủ đề ≥ 70 điểm và ≥ 60% số từ đã học. |
| Q5 | Số phút học đo thật hay ước tính? | Đo thật ở đâu đo được (mini-game, Speaking), còn lại ước tính cố định (≈1 phút/lần luyện âm, ≈10 giây/thẻ ôn từ). API ghi rõ là số ước tính. |

### 3.2 Câu hỏi khác

1. Đã chốt dùng giờ Việt Nam cho toàn hệ thống. Sau này có cần múi giờ riêng cho từng người dùng (học viên ở nước ngoài) không?
2. Kỹ năng lâu không luyện có tự giảm điểm không? Giảm nhanh hay chậm?
3. CEFR ước tính tự thay đổi theo điểm năng lực, hay chỉ đổi khi làm lại placement / bài kiểm tra định kỳ?
4. Placement không đo Speaking: điểm Speaking ban đầu lấy theo CEFR tổng, hay để trống đến phiên nói đầu tiên?
5. Kỹ năng muốn tập trung trong goal survey đang là chữ tự do: làm bảng quy đổi sang kỹ năng, hay đổi survey sang danh sách chọn sẵn?
6. Thời lượng học mỗi ngày mặc định lấy từ đâu khi học viên không khai báo?
7. Bài tập của giáo viên có luôn được ưu tiên đầu tiên trong Today Plan khi sắp hết hạn không?
8. Những mốc nào gửi thông báo đẩy (streak, hoàn thành tuần, tăng trình độ…)?
9. Ai duyệt bài Reading/Listening do AI sinh: admin hay giáo viên?
10. App mobile bản hiện tại cần được hỗ trợ tiếp trong bao lâu?
11. Backend có chạy nhiều instance không? Nếu có, các job theo lịch cần cơ chế khoá.
12. Ai phụ trách chỉnh các hệ số tính điểm năng lực sau khi có dữ liệu thật?

---

## 4. Phạm vi dự kiến

| Giai đoạn | Nội dung | Công nghệ |
|---|---|---|
| **1. Lõi (MVP)** | Bổ sung trước: tiến độ roadmap, ghi thời gian học, streak cho luyện phát âm (mục 2.2–2.4). Sau đó: ghi sự kiện học tập, không xử lý trùng; hồ sơ năng lực cho Vocabulary, Pronunciation, Speaking; Today Plan có lý do đề xuất; API hồ sơ + Today Plan | Bảng tiến độ roadmap (Flyway); bảng hàng đợi sự kiện trong DB (giống `notification_outbox`) + job nền; tính điểm bằng trung bình trượt (EMA); đề xuất theo quy tắc có trọng số, **không dùng AI**; cache Redis |
| **2. Tiến bộ & mốc** | Ảnh chụp tiến độ theo ngày, biểu đồ, mốc đạt được, thông báo khi đạt mốc | Bảng snapshot theo ngày; job cuối ngày; thông báo qua `NotificationOutboxService` có sẵn; theo dõi qua Actuator |
| **3. Reading & Listening** | Bài đọc/nghe theo chủ đề và CEFR, chấm tự động (trắc nghiệm, chép chính tả), bấm từ để thêm vào ôn tập | Spring AI sinh bài, **qua duyệt trước khi xuất bản**; Azure TTS tạo audio; Azure Blob lưu file |

**Không làm trong module này:** bài luyện Grammar riêng; nhận thời gian học do app tự báo; màn hình web cho giáo viên xem năng lực học viên; mô hình học máy.

---

## 5. API cho Mobile

| API | Giai đoạn | Mục đích |
|---|---|---|
| `GET /api/v1/learner/profile` | 1 | Trình độ và điểm từng kỹ năng |
| `GET /api/v1/recommendations/today?budgetMinutes=20` | 1 | Kế hoạch học hôm nay, kèm lý do |
| `GET /api/v1/learner/progress/summary` | 2 | Hôm nay học bao lâu, kỹ năng tăng/giảm, mốc tiếp theo |
| `GET /api/v1/learner/progress/history?range=30d` | 2 | Dữ liệu vẽ biểu đồ tiến bộ |
| `GET /api/v1/learner/milestones` | 2 | Mốc đã đạt / sắp đạt |

Mobile chỉ hiển thị và điều hướng theo `type` + `entityId` của từng hoạt động. Mobile không tự tính điểm năng lực hay thứ tự đề xuất.
