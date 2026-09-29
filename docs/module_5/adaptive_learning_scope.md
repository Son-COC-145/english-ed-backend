# Module 5 – Adaptive Learning & Progress Intelligence · Phạm vi

> Bộ tài liệu Module 5: **Phạm vi** (tài liệu này) · [Thiết kế](adaptive_learning_design.md) · [Kế hoạch thực thi](adaptive_learning_implementation_plan.md)
> Tài liệu này trả lời: **làm gì, không làm gì, thế nào là xong**. Cách làm chi tiết nằm ở tài liệu Thiết kế.

## 1. Mục tiêu

Backend biết **học viên đang ở đâu, tiến bộ thế nào và nên học gì tiếp theo**, rồi trả kết quả đã tính sẵn cho Mobile.

- Các module học hiện có (từ vựng, mini-game, phát âm, Speaking, placement, bài tập, roadmap) vẫn là nguồn dữ liệu gốc. Module 5 chỉ **tổng hợp và đề xuất**.
- Mọi con số do Backend tính theo quy tắc cố định; **không dùng AI** để quyết định học gì.
- Mobile chỉ hiển thị và điều hướng; không chứa logic tính điểm hay xếp hạng đề xuất.

---

## 2. Hiện trạng

| Module | Đang có | Dùng cho Module 5 |
|---|---|---|
| Placement | CEFR và điểm 0–100 cho Vocab, Grammar, Reading, Listening, Pronunciation | CEFR hiện tại, năng lực ban đầu |
| Goal survey | Mục tiêu học, kỹ năng muốn tập trung (chữ tự do), số phút học mỗi ngày (tuỳ chọn) | Ưu tiên đề xuất, thời lượng mặc định |
| Từ vựng (SRS) | Lịch ôn từng từ theo SM-2, trạng thái NEW → MASTERED | Từ đến hạn ôn, năng lực từ vựng |
| Mini-game | Lượt chơi theo chủ đề: điểm 0–100, thời lượng | Năng lực từ vựng, thời gian học |
| Phát âm | Log từng lần luyện, mức thành thạo từng âm | Âm yếu, năng lực phát âm |
| Speaking | Phiên hội thoại có điểm hoàn thành nhiệm vụ, lưu loát, ngữ điệu | Năng lực nói, thời gian học |
| Roadmap | Lộ trình theo tuần/chủ đề sinh sau placement, **chưa có tiến độ** | Lộ trình dài hạn |
| Bài tập | Học viên xem bài chưa làm, đã nộp, điểm | Bài sắp hết hạn |
| Gamification | XP, streak, Daily Mission (gợi ý từ vựng theo lịch lớp) | Mốc streak |

### Điểm còn thiếu

| # | Vấn đề | Hướng xử lý |
|---|---|---|
| 1 | Reading, Listening, Grammar **chưa có bài luyện** (chỉ có trong placement); placement không đo Speaking | Hiển thị với nhãn "Theo placement", không đưa vào đề xuất. Bài đọc/nghe làm sau MVP. |
| 2 | Roadmap **chưa theo dõi tiến độ** (luôn "tuần 1, chưa xong module nào") | Làm tiến độ roadmap là **việc đầu tiên** |
| 3 | **Chưa ghi thời gian học** | Ghi thời lượng cho mỗi hoạt động, tách đo thật / ước tính |
| 4 | Có hai "kế hoạch hôm nay" nếu giữ Daily Mission | Today Plan thay Daily Mission |

---

## 3. Chức năng

| Mã | Chức năng | Mô tả | MVP |
|---|---|---|:-:|
| F1 | Tiến độ roadmap | Biết học viên đã xong module/tuần nào của roadmap, đang ở tuần nào. Module hoàn thành khi **làm hết** nội dung của module. Roadmap sinh lại (làm lại placement) thì tính lại theo roadmap mới. | ✓ |
| F2 | Sự kiện học tập | Mỗi hoạt động học (ôn từ, lượt mini-game, luyện âm, phiên nói, placement, chấm bài) ghi một sự kiện, không bao giờ bị tính hai lần. Các chức năng khác đều cập nhật từ sự kiện này. | ✓ |
| F3 | Hồ sơ năng lực | CEFR (theo placement) và điểm năng lực 0–100 từng kỹ năng, kèm độ tin cậy và xu hướng. Cập nhật liên tục cho **Vocabulary, Pronunciation, Speaking**; Grammar/Reading/Listening lấy theo placement. | ✓ |
| F4 | Thời gian học | Số phút học mỗi ngày, tách phần đo thật và phần ước tính. | ✓ |
| F5 | Today Plan | Danh sách hoạt động nên học hôm nay trong số phút cho trước, có lý do đề xuất. Nguồn đề xuất: từ đến hạn ôn, module roadmap tiếp theo, âm yếu, kỹ năng yếu, bài tập sắp hết hạn, từ mới theo lịch lớp. | ✓ |
| F6 | Tiến bộ theo ngày | Ảnh chụp năng lực và thời gian học mỗi ngày; tóm tắt hôm nay học bao lâu, kỹ năng nào tăng/giảm, mốc tiếp theo; lịch sử để vẽ biểu đồ. | |
| F7 | Mốc (milestones) | Streak 7/14/30 ngày, hoàn thành module/tuần/roadmap, kỹ năng vượt 60/80 điểm, số hoạt động đã làm; thông báo khi đạt mốc. | |
| F8 | Reading & Listening | Bài đọc/nghe theo chủ đề và CEFR, chấm tự động, nội dung sinh bằng AI và được duyệt. | |

**Hoàn thành ≠ thành thạo:** F1 đo học viên đã **làm** được bao nhiêu nội dung; F3/F6 đo học viên **giỏi lên** bao nhiêu. Module đã hoàn thành nhưng điểm thấp vẫn được Today Plan đề xuất luyện thêm.

---

## 4. Trong phạm vi / Ngoài phạm vi

**Trong phạm vi MVP:** F1–F5 và 2 API cho Mobile: Today Plan, Learner Profile; API tiến độ roadmap trả số thật.

**Sau MVP (vẫn thuộc Module 5):** F6, F7 và các API Progress Summary, Progress History, Milestones; F8.

**Ngoài phạm vi Module 5:**
- Backfill dữ liệu học cũ (hệ thống chỉ lên production sau khi hoàn thành dự án).
- Bài luyện Grammar riêng.
- Tóm tắt huấn luyện bằng AI, bài kiểm tra định kỳ đổi CEFR, phân tích phản hồi đề xuất để chỉnh trọng số.
- Nhận thời gian học do app tự báo.
- Màn hình web cho giáo viên xem năng lực học viên.
- Mô hình học máy.

---

## 5. Quyết định đã chốt

1. Dùng giờ Việt Nam (`Asia/Ho_Chi_Minh`) cho mọi tính toán theo ngày.
2. CEFR lấy từ placement gần nhất, **không** tự đổi theo điểm năng lực.
3. Module roadmap **làm hết mới hoàn thành**, không có ngưỡng điểm.
4. Tiến độ roadmap gắn với từng phiên bản roadmap; roadmap sinh lại thì tính lại, không tự kế thừa.
5. Sự kiện học tập xử lý theo kiểu hàng đợi trong DB, giống đồng bộ `sync_knowledge_base` của DigiWorld.
6. Thời gian học lưu tách đo thật / ước tính.
7. Kỹ năng lâu không luyện **không** tự giảm điểm.
8. Today Plan thay Daily Mission (Daily Mission giữ tạm cho app bản cũ).
9. Mobile điều hướng theo `type` + `target`; lý do đề xuất trả dạng mã (`reasonCode` + `reasonParams`), Mobile tự dịch.
10. Không backfill dữ liệu cũ.

---

## 6. Câu hỏi mở

| # | Câu hỏi | Đề xuất mặc định (dùng nếu chưa chốt) | Cần trước |
|---|---|---|---|
| O1 | Reading/Listening/Grammar có hiển thị trong hồ sơ năng lực khi chưa có bài luyện? | Có, nhãn "Theo placement" | F3 |
| O2 | Điểm giáo viên chấm có tính vào năng lực? | MVP không; sau này thay cho điểm tự động của cùng bài làm | F3 |
| O3 | Placement không đo Speaking: điểm Speaking ban đầu? | Để trống đến phiên nói đầu tiên | F3 |
| O4 | Kỹ năng muốn tập trung (chữ tự do): bảng quy đổi hay đổi survey sang danh sách chọn? | Bảng quy đổi cố định trong code | F5 |
| O5 | Thời lượng mặc định của Today Plan khi học viên không khai báo? | 15 phút | F5 |
| O6 | Bài tập sắp hết hạn (< 24 giờ) có luôn đứng đầu Today Plan? | Có | F5 |
| O7 | Module phát âm tuần 1 có giữ cả 44 âm không (làm hết mới hoàn thành)? | Giữ 44 âm; rút gọn sau nếu quá nặng | F1 |
| O8 | Những mốc nào gửi thông báo đẩy? | Streak, hoàn thành tuần, hoàn thành roadmap | F7 |
| O9 | App mobile bản hiện tại cần hỗ trợ tiếp bao lâu? | Đến hết đợt phát hành đầu tiên của Module 5 | F5 |
| O10 | Backend có chạy nhiều instance? | Một instance; thiết kế vẫn an toàn khi nhiều instance | F2 |
| O11 | Ai phụ trách chỉnh các hệ số tính điểm sau khi có dữ liệu thật? | Chưa có đề xuất | F3 |

---

## 7. Tiêu chí hoàn thành MVP (Definition of Done)

1. Ôn từ, hoàn thành lượt mini-game, luyện phát âm, hoàn thành phiên nói, hoàn thành placement và giáo viên chấm bài đều tạo **đúng một** sự kiện học tập, kể cả khi client gửi lại request hoặc worker xử lý lại.
2. `GET /onboarding/roadmap/progress` trả tuần hiện tại, số module/tuần đã xong **theo dữ liệu thật**; module hoàn thành khi làm hết nội dung.
3. Làm lại placement → roadmap mới có tiến độ tính theo nội dung mới, roadmap cũ giữ làm lịch sử.
4. Điểm năng lực của Vocabulary, Pronunciation, Speaking thay đổi sau mỗi hoạt động tương ứng.
5. Thời gian học hôm nay đúng theo giờ Việt Nam, tách đo thật / ước tính.
6. `GET /recommendations/today` trả kế hoạch đúng ngân sách phút, mỗi mục có `type`, `target`, `reasonCode`; kế hoạch **thay đổi** sau khi học viên học xong một hoạt động.
7. `GET /learner/profile` trả CEFR, điểm năng lực và xu hướng từng kỹ năng.
8. Mobile không chứa logic tính điểm hay xếp hạng đề xuất.
9. Có unit test cho các quy tắc tính toán và test tích hợp Postgres cho chống trùng sự kiện.

## 8. Kịch bản demo

| Bước | Hành động | Kết quả mong đợi |
|---|---|---|
| 1 | Học viên mới làm placement | Có CEFR, hồ sơ năng lực ban đầu, roadmap với tiến độ 0% |
| 2 | Mở app | Today Plan gợi ý module roadmap tuần 1 và luyện âm, có lý do |
| 3 | Luyện phát âm vài âm | Điểm Pronunciation đổi; tiến độ module phát âm tăng; thời gian học hôm nay tăng |
| 4 | Quay lại màn hình chính | Today Plan được tính lại (âm vừa luyện điểm thấp được ưu tiên) |
| 5 | Học hết từ của chủ đề tuần 1 | Module từ vựng tuần 1 hoàn thành (100%) |
| 6 | Hoàn thành một phiên nói | Điểm Speaking xuất hiện; streak tính ngày học |
| 7 | Gửi lại request cũ (giả lập mạng chập chờn) | Không có sự kiện hay điểm nào bị cộng hai lần |
| 8 | Làm lại placement | Roadmap mới, tiến độ tính lại theo nội dung mới |
