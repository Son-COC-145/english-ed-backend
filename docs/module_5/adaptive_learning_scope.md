# Module 5 – Adaptive Learning & Progress Intelligence

> Người tạo: Nguyễn Đức Mạnh · Được tạo bởi: Claude Opus 5.5 · Ngày tạo: 29/09/2026 · Cập nhật: 29/09/2026 (tích hợp góp ý review "Các điểm cần chỉnh trước khi triển khai")
> Trạng thái: Bản nháp, chờ nhóm chốt · Nhánh tham chiếu: `main` · Migration mới nhất: `V57`

**Mục tiêu:** Backend biết học viên đang ở trình độ nào, tiến bộ ra sao và nên học gì tiếp theo, rồi trả kết quả đã tính sẵn cho Mobile. Các module hiện có vẫn là nguồn dữ liệu gốc. Module 5 chỉ tổng hợp dữ liệu từ các module đó.

**Nguyên tắc:**
- Mọi module học phát **sự kiện học tập** một lần duy nhất; các phần tổng hợp (tiến độ roadmap, năng lực, thời gian học, đề xuất, mốc) đều đọc từ sự kiện đó.
- Mọi con số do Backend tính theo quy tắc cố định; không dùng AI để quyết định học gì.
- Mobile chỉ hiển thị và điều hướng; không chứa logic tính điểm hay xếp hạng đề xuất.

---

## 1. Hiện trạng

| Module | Đang có | Dùng được cho Module 5 |
|---|---|---|
| Placement | Lưu CEFR và điểm 0–100 cho 5 kỹ năng: Vocab, Grammar, Reading, Listening, Pronunciation | CEFR hiện tại, hồ sơ năng lực ban đầu |
| Goal survey | Mục tiêu học, kỹ năng muốn tập trung (chữ tự do), số phút học mỗi ngày (tuỳ chọn) | Ưu tiên đề xuất theo mục tiêu |
| Từ vựng (SRS) | Lịch ôn từng từ theo SM-2, trạng thái NEW → MASTERED | Gợi ý "từ đến hạn ôn" |
| Mini-game | Lượt chơi theo chủ đề: điểm 0–100, thời lượng | Đo năng lực từ vựng, thời gian học |
| Phát âm | Log từng lần luyện; tính được mức thành thạo từng âm | Gợi ý "âm yếu" |
| Speaking | Phiên hội thoại có điểm hoàn thành nhiệm vụ, lưu loát, ngữ điệu | Đo năng lực nói, thời gian học |
| Roadmap | Lộ trình theo tuần/chủ đề sinh sau placement (JSON), chưa có tiến độ | Lộ trình dài hạn |
| Bài tập | Học viên xem được bài chưa làm, bài đã nộp và điểm | Gợi ý "bài sắp hết hạn" |
| Gamification | XP, streak (tính cho mini-game, ôn từ, Speaking), Daily Mission (gợi ý từ vựng theo lịch lớp) | Mốc streak |
| Hạ tầng | Giờ nghiệp vụ thống nhất `Asia/Ho_Chi_Minh`, Redis, xử lý nền theo lịch, bảng hàng đợi dùng DB, Spring AI, Azure TTS/Blob, Actuator | Xử lý sự kiện, cache, sinh nội dung, số liệu theo ngày |

---

## 2. Điểm còn thiếu và giải pháp

1. **Reading, Listening, Grammar chưa có bài luyện.** Ba kỹ năng này chỉ xuất hiện trong placement. Câu hỏi Reading/Listening trong ngân hàng mới là dữ liệu mẫu (~30 câu mỗi loại). Placement cũng không đo Speaking.
   → Điểm của 3 kỹ năng này không bao giờ thay đổi, và không có gì để đề xuất cho chúng.
   **Giải pháp:** trước mắt hiển thị 3 kỹ năng với nhãn "Theo placement", không đưa vào Today Plan. Sau MVP làm một engine bài đọc/nghe hiểu dùng chung cho Reading và Listening (mục 7, bước 10). Grammar tạm dùng lỗi ngữ pháp từ phiên Speaking làm tín hiệu tham khảo.
2. **Roadmap chưa theo dõi tiến độ.** API tiến độ luôn trả "tuần 1, chưa xong module nào". Module không có mã định danh, chưa có chỗ lưu module đã hoàn thành.
   → Chưa biết học viên đang ở tuần/module nào.
   **Giải pháp:** chuẩn hoá tiến độ roadmap là **việc đầu tiên** (mục 3).
3. **Chưa ghi thời gian học.** Cột tổng số phút học có nhưng không được ghi. Luyện phát âm và ôn từ không lưu thời lượng.
   → Chưa trả lời được "hôm nay học bao lâu".
   **Giải pháp:** mỗi sự kiện học tập mang thời lượng và nguồn gốc (đo thật / ước tính); bảng theo ngày lưu **tách riêng** số giây đo thật và số giây ước tính (mục 4.4).
4. **Có hai "kế hoạch hôm nay".** Daily Mission đã gợi ý từ vựng. Nếu thêm Today Plan mà không gộp, app sẽ có hai danh sách mâu thuẫn nhau.
   **Giải pháp:** Today Plan thay thế Daily Mission. Logic "từ mới theo tuần của lớp" chuyển thành một nguồn đề xuất trong Today Plan. Endpoint `/gamification/daily-mission` giữ tạm cho app bản cũ (trả phần từ vựng lấy từ Today Plan), sau đó bỏ.

---

## 3. Việc đầu tiên: chuẩn hoá tiến độ Roadmap

Module 5 cần biết học viên **đang ở tuần/module nào** để đề xuất module tiếp theo, tính tiến bộ và phát hiện mốc "hoàn thành tuần".

### 3.1 Roadmap hiện nay

- Roadmap được sinh sau placement, lưu dạng JSON trong `student_onboarding.roadmap_json`, kèm `roadmap_generation_version`. Sinh lại khi học viên làm lại placement (có thể đổi CEFR).
- Mỗi **tuần** ứng với một chủ đề (tối đa 5 tuần). Module trong tuần:
  - `VOCABULARY`: từ vựng của chủ đề (`topicId`, `itemCount` = số từ).
  - `SPEAKING`: chỉ khi học viên chọn mục tiêu "Giao tiếp" và chủ đề có kịch bản nói ở CEFR của học viên.
  - `IPA_PRONUNCIATION`: chỉ ở tuần 1, 44 âm.
- Module chỉ có `type`, `title`, `topicId`, `itemCount`, `cefrLevel`, **không có mã định danh**. `GET /onboarding/roadmap/progress` gán cứng `currentWeek = 1`, `completedModules = 0`.

### 3.2 Định danh module và phiên bản roadmap

- **`moduleKey`** trong một roadmap: `VOCABULARY:{topicId}`, `SPEAKING:{topicId}`, `IPA_PRONUNCIATION:FOUNDATION`. Thêm vào `RoadmapModule` khi sinh; roadmap cũ chưa có thì tính khi đọc từ `type` + `topicId`.
- **Tiến độ gắn với phiên bản roadmap:** `UNIQUE(student_id, roadmap_version, module_key)`, trong đó `roadmap_version` = `roadmap_generation_version` hiện có.
- **Không tự kế thừa tiến độ** chỉ vì trùng `moduleKey`. Khi roadmap sinh lại:
  - Không sao chép dòng tiến độ cũ. Dòng của phiên bản cũ giữ nguyên làm lịch sử.
  - Tiến độ của phiên bản mới được **tính lại từ sự kiện học tập** theo điều kiện của module mới (đúng chủ đề **và** đúng CEFR của module). Ví dụ: từ đã học vẫn được tính vì là kết quả thật; nhưng một phiên nói A2 không làm hoàn thành module Speaking B1 cùng chủ đề.

### 3.3 Ngữ nghĩa tiến độ và trạng thái

Tránh trường hợp `status = COMPLETED` nhưng thanh tiến độ chỉ 20–60%. Chọn **hướng tách rõ** các đại lượng:

| Trường (lưu DB) | Ý nghĩa |
|---|---|
| `coverage_percent` | Mức phủ nội dung module (số từ đã học / tổng từ, số âm đạt / 44…) |
| `assessment_passed` | Đã đạt bài đánh giá của module chưa (lượt chơi ≥ ngưỡng, phiên nói đạt…) |
| `status` | `NOT_STARTED` / `IN_PROGRESS` / `COMPLETED` |
| `completed_at`, `last_evidence_at` | Thời điểm hoàn thành / lần có kết quả gần nhất |

**Quy tắc hiển thị (API):** nếu `status = COMPLETED` thì `progressPercent = 100`; ngược lại `progressPercent = min(coverage_percent, 99)`. API trả thêm `coveragePercent` và `assessmentPassed` để Mobile hiện chi tiết nếu cần.

**Điều kiện hoàn thành** (ngưỡng để trong config):

| Loại | `coverage_percent` | `assessment_passed` | `COMPLETED` khi |
|---|---|---|---|
| `VOCABULARY` | Từ của chủ đề đạt LEARNING trở lên / `itemCount` | Có lượt mini-game của chủ đề ≥ 70 điểm | coverage ≥ 60% **và** assessment passed |
| `SPEAKING` | Số kịch bản của chủ đề đã có phiên hoàn thành / số kịch bản | Có phiên nói hoàn thành (đúng CEFR) với điểm hoàn thành nhiệm vụ ≥ 60 | assessment passed |
| `IPA_PRONUNCIATION` | Số âm có điểm trung bình ≥ 60 / 44 | ≥ 10 âm đạt trung bình ≥ 60 | assessment passed |

Tuần hoàn thành khi mọi module trong tuần `COMPLETED`. Tuần hiện tại là tuần đầu tiên còn module chưa xong; module gợi ý là module đầu tiên chưa xong của tuần đó.

### 3.4 Cập nhật tiến độ

- `RoadmapProgressService` là **một consumer của sự kiện học tập** (mục 4), không được các module gọi trực tiếp. Mỗi sự kiện chỉ tính lại các module liên quan (cùng `topicId`, hoặc module IPA) của phiên bản roadmap hiện tại.
- Khi module chuyển sang `COMPLETED`, phát sự kiện `ROADMAP_MODULE_COMPLETED` (và `ROADMAP_WEEK_COMPLETED` khi cả tuần xong), đúng một lần nhờ khoá chống trùng.
- API (chỉ thêm trường, tương thích ngược): `GET /onboarding/roadmap` thêm `moduleKey`, `status`, `progressPercent`, `coveragePercent`, `assessmentPassed` cho từng module; `GET /onboarding/roadmap/progress` trả số thật.

---

## 4. Nền tảng: Sự kiện học tập (Learning Event / Outbox)

Dựng **ngay từ đầu**, trước khi hook các module, để mỗi module chỉ tích hợp một lần.

```text
Vocabulary · Mini-game · Pronunciation · Speaking · Placement · Assignment · Roadmap
        │  ghi sự kiện CÙNG transaction với nghiệp vụ (outbox)
        ▼
   learning_events  ──worker──►  Roadmap Progress · Learner Model · Study Time · Recommendation cache · Milestones
```

### 4.1 Ghi sự kiện

- Sự kiện được **ghi vào bảng trong cùng transaction** với dữ liệu nghiệp vụ (transactional outbox): nghiệp vụ rollback thì sự kiện cũng không tồn tại; nghiệp vụ commit thì sự kiện chắc chắn còn, kể cả khi server tắt ngay sau đó.
- Worker xử lý sau commit: `@TransactionalEventListener(AFTER_COMMIT)` để đánh thức ngay + job `@Scheduled` quét định kỳ, claim bằng token như `notification_outbox`. Retry có backoff; quá số lần thì `DEAD` và có log/metric.

### 4.2 Cấu trúc bảng `learning_events`

| Cột | Ghi chú |
|---|---|
| `event_id` (UUID) | Khoá chính |
| `student_id` | |
| `event_type` | `PLACEMENT_COMPLETED`, `VOCAB_REVIEWED`, `VOCAB_ROUND_COMPLETED`, `PRONUNCIATION_PRACTICED`, `SPEAKING_SESSION_EVALUATED`, `ASSIGNMENT_GRADED`, `ROADMAP_MODULE_COMPLETED`, `ROADMAP_WEEK_COMPLETED` |
| `source`, `source_reference` | Nguồn và id kết quả gốc (bảng 4.3) |
| `skill` | `VOCABULARY` / `PRONUNCIATION` / `SPEAKING` / … (có thể null) |
| `entity_type`, `entity_id` | Đối tượng học: `TOPIC`, `PHONEME`, `SCENARIO`, `ASSIGNMENT`, `ROADMAP_MODULE`… |
| `score` | 0–100 (có thể null) |
| `duration_seconds`, `duration_source` | Thời lượng và nguồn `MEASURED` / `ESTIMATED` |
| `payload` (jsonb) | Dữ liệu riêng từng loại |
| `schema_version` | Phiên bản cấu trúc `payload` |
| `occurred_at` | Thời điểm học (giờ HCM) |
| `status`, `retry_count`, `processed_at`, `last_error`, `claim_token` | Trạng thái xử lý |

**Chống trùng:** `UNIQUE(source, source_reference, event_type)`.

### 4.3 `source_reference` theo từng module

| Sự kiện | `source` | `source_reference` |
|---|---|---|
| Hoàn thành placement | `PLACEMENT_SESSION` | id phiên placement |
| Ôn một từ (SRS) | `VOCAB_REVIEW` | `attemptId` của request (không có bảng log riêng cho lần ôn) |
| Hoàn thành lượt mini-game | `MINIGAME_ROUND` | id lượt chơi |
| Chấm phát âm | `PRONUNCIATION_LOG` | id log luyện phát âm |
| Chấm xong phiên nói | `SPEAKING_SESSION` | id phiên nói (chỉ khi `COMPLETED`) |
| Giáo viên chấm bài | `ASSIGNMENT_SUBMISSION` | `{submissionId}:{gradingRevision}` |
| Hoàn thành module/tuần | `ROADMAP` | `{studentId}:{roadmapVersion}:{moduleKey}` / `…:WEEK:{n}` |

Ưu tiên id của **kết quả gốc** trong DB. `attemptId` chỉ dùng cho ôn từ vì đó là định danh duy nhất của một lần ôn; bảng `idempotency_keys` bị xoá sau 24 giờ nhưng khoá chống trùng trong `learning_events` được giữ lâu dài.

### 4.4 Xử lý đúng một lần cho từng consumer

- Mỗi consumer ghi nhận đã áp dụng sự kiện vào bảng `learning_event_applications(event_id, consumer)` (khoá chính kép) **trong cùng transaction** với thay đổi của nó. Sự kiện xử lý lại (retry, chạy lại job) sẽ bỏ qua consumer đã áp dụng, nên mastery (EMA) và thời gian học không bị cộng hai lần.
- Một consumer lỗi chỉ consumer đó được thử lại, không ảnh hưởng consumer khác.
- **Thời gian học:** bảng `student_daily_activity(student_id, activity_date, measured_seconds, estimated_seconds, activity_count)`. Đo thật: lượt mini-game, phiên Speaking. Ước tính (config): ≈60 giây/lần luyện phát âm, ≈10 giây/thẻ ôn từ. Trần 15 phút mỗi hoạt động. API trả `totalStudyMinutes`, `measuredMinutes`, `estimatedMinutes`.

---

## 5. Hợp đồng API cho Mobile

### 5.1 Today Plan

`GET /api/v1/recommendations/today?budgetMinutes=20`

```json
{
  "generatedAt": "2026-09-29T08:00:00",
  "profileVersion": 17,
  "estimatedMinutes": 19,
  "activities": [
    {
      "recommendationId": "rec-123",
      "type": "ASSIGNMENT",
      "title": "Bài tập tuần 3",
      "estimatedMinutes": 15,
      "target": { "courseId": 8, "assignmentId": 21 },
      "reasonCode": "ASSIGNMENT_DUE",
      "reasonParams": { "daysRemaining": 1 }
    }
  ]
}
```

- Mobile điều hướng theo `type` + `target`. Cấu trúc `target` cố định theo từng `type`:

| `type` | `target` |
|---|---|
| `VOCABULARY_REVIEW` | `{ "mode": "DUE" }` |
| `VOCABULARY_TOPIC` | `{ "topicId" }` |
| `PRONUNCIATION` | `{ "phonemeId" }` |
| `SPEAKING` | `{ "scenarioId" }` |
| `ASSIGNMENT` | `{ "courseId", "assignmentId" }` |
| `ROADMAP_MODULE` | `{ "roadmapVersion", "moduleKey" }` |

- **`reasonCode` + `reasonParams` là hợp đồng chính**; Mobile tự dịch sang tiếng Việt/Anh. Backend có thể trả thêm `reason` (câu tiếng Việt) chỉ làm fallback. Danh sách `reasonCode`: `SRS_DUE`, `WEAK_SKILL`, `WEAK_PHONEME`, `ROADMAP_NEXT`, `ASSIGNMENT_DUE`, `GOAL_FOCUS`, `KEEP_STREAK`.
- `title` chỉ dùng cho hoạt động có tên nội dung thật (tên bài tập, chủ đề, kịch bản). Hoạt động chung như "ôn từ đến hạn" để Mobile tự đặt tiêu đề theo `type`.

### 5.2 Learner Profile và CEFR

`GET /api/v1/learner/profile` trả `cefr`, `cefrSource`, `cefrAssessedAt`, mastery tổng và từng kỹ năng (`mastery`, `confidence`, `source`, `trend`).

- **CEFR không tự tăng/giảm theo mastery (EMA).** CEFR lấy từ placement gần nhất; sau này từ bài kiểm tra định kỳ (checkpoint) đủ tin cậy. Mastery chỉ dùng cho đề xuất và theo dõi kỹ năng.

### 5.3 API khác

| API | Mục đích |
|---|---|
| `GET /api/v1/learner/progress/summary` | Hôm nay học bao lâu (đo thật / ước tính), kỹ năng tăng/giảm, tuần roadmap hiện tại, mốc tiếp theo |
| `GET /api/v1/learner/progress/history?range=30d` | Snapshot theo ngày để vẽ biểu đồ |
| `GET /api/v1/learner/milestones` | Mốc đã đạt / sắp đạt |

---

## 6. Câu hỏi mở

### 6.1 Cần chốt trước khi code (ảnh hưởng DB/API)

| # | Câu hỏi | Đề xuất trong tài liệu này |
|---|---|---|
| Q1 | Định danh và phiên bản roadmap | `UNIQUE(student_id, roadmap_version, module_key)`; không kế thừa theo `moduleKey`, tính lại từ sự kiện theo điều kiện module mới (mục 3.2) |
| Q2 | Ngữ nghĩa tiến độ | Tách `coverage_percent` / `assessment_passed` / `status`; `COMPLETED` ⇒ `progressPercent = 100` (mục 3.3) |
| Q3 | Sự kiện học tập và chống trùng | Outbox ghi cùng transaction; `UNIQUE(source, source_reference, event_type)`; bảng áp dụng theo consumer (mục 4) |
| Q4 | Hợp đồng Today Plan | `type` + `target` + `reasonCode` + `reasonParams` (mục 5.1) |
| Q5 | Ngưỡng hoàn thành từng loại module | 70 điểm / 60% từ (Vocabulary), 60 điểm (Speaking), 10 âm ≥ 60 (IPA) — cần nhóm xác nhận |

### 6.2 Cần chốt trước khi làm phần tương ứng

1. Reading/Listening/Grammar có hiển thị trong hồ sơ năng lực khi chưa có bài luyện không? (Đề xuất: có, nhãn "Theo placement", không vào Today Plan.)
2. Điểm giáo viên chấm có tính vào năng lực không? (Đề xuất: MVP không; sau này điểm giáo viên thay cho điểm tự động của cùng bài làm, không cộng thêm.)
3. Kỹ năng lâu không luyện có tự giảm điểm không? Giảm nhanh hay chậm?
4. Placement không đo Speaking: điểm Speaking ban đầu lấy theo CEFR tổng, hay để trống đến phiên nói đầu tiên?
5. Kỹ năng muốn tập trung trong goal survey đang là chữ tự do: làm bảng quy đổi, hay đổi survey sang danh sách chọn sẵn?
6. Thời lượng học mỗi ngày mặc định lấy từ đâu khi học viên không khai báo?
7. Bài tập của giáo viên có luôn đứng đầu Today Plan khi sắp hết hạn không?
8. Những mốc nào gửi thông báo đẩy?
9. Bài kiểm tra định kỳ (checkpoint) để đổi CEFR làm ở giai đoạn nào?
10. App mobile bản hiện tại cần được hỗ trợ tiếp trong bao lâu?
11. Backend có chạy nhiều instance không? Nếu có, worker và job theo lịch cần cơ chế khoá.
12. Ai phụ trách chỉnh các hệ số tính điểm năng lực sau khi có dữ liệu thật?

**Đã chốt:** dùng giờ Việt Nam cho toàn hệ thống; CEFR lấy từ placement/checkpoint, không theo EMA; thời gian học lưu tách đo thật / ước tính; Today Plan thay Daily Mission.

---

## 7. Thứ tự triển khai

| Bước | Nội dung | Công nghệ / cách làm |
|---|---|---|
| 1 | **Chuẩn hoá tiến độ Roadmap:** `moduleKey`, bảng `roadmap_module_progress` theo phiên bản, ngữ nghĩa hoàn thành | Flyway; thêm trường vào JSON roadmap; ngưỡng trong `@ConfigurationProperties` |
| 2 | **Learning Event / Outbox:** bảng `learning_events`, `learning_event_applications`, worker, chống trùng, retry | Outbox trong DB (giống `notification_outbox`); `@TransactionalEventListener(AFTER_COMMIT)` + `@Scheduled` |
| 3 | **Hook các module:** Vocabulary (ôn từ), Mini-game, Pronunciation, Speaking, Placement, Assignment, Roadmap | Một `LearningEventPublisher` dùng chung, mỗi module gọi một dòng trong transaction nghiệp vụ |
| 4 | **Các bảng tổng hợp:** hồ sơ năng lực, trạng thái từng kỹ năng, hoạt động theo ngày, tiến độ roadmap | Consumer Java thuần; EMA với hệ số trong config; `@Version` chống ghi đè |
| 5 | **Recommendation Engine:** nguồn đề xuất, chấm điểm, đa dạng hoá, `reasonCode` | Interface `CandidateSource` (mỗi nguồn một bean); trọng số trong config; không dùng AI |
| 6 | **Today Plan API** | REST + springdoc; cache Redis theo `profileVersion` |
| 7 | **Learner Profile API** | REST |
| 8 | **Progress snapshot / history** | Bảng snapshot theo ngày, upsert `ON CONFLICT`; job cuối ngày theo giờ HCM |
| 9 | **Milestones + thông báo** | Quy tắc mốc (streak, hoàn thành tuần, mastery vượt ngưỡng); thông báo qua `NotificationOutboxService` |
| 10 | **Reading & Listening** (sau MVP) | Spring AI sinh bài, duyệt trước khi xuất bản; Azure TTS tạo audio; Azure Blob lưu file |

**MVP = bước 1–7:** Backend nhận sự kiện học tập từ các module → cập nhật tiến độ roadmap, năng lực, thời gian học → trả Today Plan có lý do và hồ sơ năng lực.

**Không làm trong module này:** bài luyện Grammar riêng; nhận thời gian học do app tự báo; màn hình web cho giáo viên xem năng lực học viên; mô hình học máy.
