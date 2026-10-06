# Module 5 – Adaptive Learning & Progress Intelligence · Kế hoạch thực thi

> Kiến trúc đã chốt: **Roadmap là kế hoạch ổn định theo tuần; Today Plan là kế
> hoạch thích ứng theo ngày**. Không đưa ngày học hoặc ngân sách phút vào JSON Roadmap.

## 1. Trạng thái hiện tại

| Phần | Trạng thái |
|---|---|
| Goal Survey mã cố định | Hoàn thành, có parser tương thích dữ liệu cũ |
| Roadmap generation bền vững | Hoàn thành, có wake-up, retry và chống roadmap rỗng |
| Roadmap snapshot/progress `V59` | Hoàn thành nền tảng |
| Learning Event `V60`/`V61` | Hoàn thành hạ tầng queue/outbox/worker; chưa đủ hook nghiệp vụ |
| Weekly Roadmap contract v2 | Hoàn thành |
| RoadmapProgressConsumer | Hoàn thành |
| Learner Model / Study Time | Hoàn thành backend MVP (`V62`/`V63`) |
| Today Plan | Hoàn thành persisted daily plan MVP (`V64`) |

Migration đã được cấp phát:

- `V59__roadmap_module_progress.sql`
- `V60__learning_events.sql`
- `V61__rename_learning_event_dead_status.sql`
- `V62__learner_model.sql`
- `V63__student_daily_activity.sql`
- `V64__today_plans.sql`

Migration tiếp theo phải bắt đầu từ `V65`. Roadmap tuần hiện tại không cần migration mới.

## 2. Giai đoạn A – Weekly Roadmap

### A1. Contract và generation

1. Giữ cấu trúc `Roadmap -> Week -> Module`.
2. Mỗi module có `moduleKey`, `contentItemIds`, `contentVersion`.
3. Roadmap có `schemaVersion`, `currentCefrLevel`, `targetCefrLevel`.
4. `dailyStudyMinutes` chỉ được Goal Survey lưu để Today Plan sử dụng; không chia
   module Roadmap thành ngày.
5. `RoadmapJobService` từ chối:
   - roadmap hoặc tuần rỗng;
   - module rỗng hoặc type không hỗ trợ;
   - `moduleKey` trùng/quá dài;
   - snapshot có ID trùng hoặc `itemCount` không khớp.

### A2. Tiến độ và mở khóa

1. Đọc hoàn thành bằng ba batch query cho Vocabulary, Speaking và Pronunciation.
2. Batch-upsert `roadmap_module_progress`.
3. Tiến độ module = `doneCount / totalCount`.
4. Tiến độ tuần/toàn roadmap = `sum(doneCount) / sum(totalCount)`.
5. Tuần 1 mở; tuần N mở khi tuần N-1 hoàn thành.
6. Mọi module trong tuần đang mở đều có thể truy cập; Today Plan chọn thứ tự ưu tiên.
7. API trả `status`, `isCompleted`, `isAccessible`, `unlockCondition`,
   `currentWeek` và `currentModuleKey`.
8. Read API vẫn gọi recalculation từ bảng nguồn làm correctness fallback.

### A3. Learning Event integration

1. `RoadmapJobService.markReady` ghi `ROADMAP_GENERATED` cùng transaction.
2. `RoadmapProgressConsumer` hỗ trợ:
   - `ROADMAP_GENERATED`
   - `VOCAB_REVIEWED`
   - `VOCAB_ROUND_COMPLETED`
   - `PRONUNCIATION_PRACTICED`
   - `SPEAKING_SESSION_EVALUATED`
3. Khi đã hoàn thành, consumer ghi idempotent derived event:
   - `ROADMAP_MODULE_COMPLETED`
   - `ROADMAP_WEEK_COMPLETED`
   - `ROADMAP_COMPLETED`
4. Publisher và consumer Roadmap phải được deploy cùng release để event không bị xử
   lý trước khi consumer tồn tại.

### A4. Definition of Done

- Unit test generation, legacy resolver, weighted progress, unlock và empty content.
- Contract test bảo đảm response chỉ có `week -> module`, không có day fields.
- Test `ROADMAP_GENERATED` được phát đúng một lần theo source reference.
- Test consumer phát derived events với stable source reference và causation ID.
- `mvn package` xanh.
- Smoke test Azure/Supabase:
  1. hoàn thành Goal Survey/Placement;
  2. roadmap chuyển `PENDING -> PROCESSING -> READY`;
  3. bảng `learning_events` có `ROADMAP_GENERATED -> DONE`;
  4. học một nội dung rồi gọi progress, `doneCount` thay đổi;
  5. hoàn thành module/tuần, derived event chỉ có một dòng.

## 3. Giai đoạn B – Hook hoạt động học

Backend owner của từng module gắn `LearningEventOutboxService.saveOutbox()` trong đúng
transaction nghiệp vụ:

| Module | Event |
|---|---|
| Placement | `PLACEMENT_COMPLETED` |
| Vocabulary review | `VOCAB_REVIEWED` |
| Mini-game | `VOCAB_ROUND_COMPLETED` |
| IPA assessment | `PRONUNCIATION_PRACTICED` |
| Speaking | `SPEAKING_SESSION_EVALUATED` |
| Assignment | `ASSIGNMENT_GRADED` |

Mỗi hook phải có stable `sourceReference`, test rollback và test gọi lại không sinh bản
ghi trùng. Không bật publisher production nếu consumer cần thiết chưa được deploy.

## 4. Giai đoạn C – Learner Model và Study Time

1. `V62__learner_model.sql`: `learner_profile`, `learner_skill_state`.
2. `LearnerModelConsumer`: placement bootstrap, EMA, confidence, trend và
   `profileVersion`.
3. `V63__student_daily_activity.sql`.
4. `StudyTimeConsumer`: thời lượng đo thật/ước tính theo `Asia/Ho_Chi_Minh`.
5. `PlanCacheConsumer`: tăng profile version khi đầu vào Today Plan thay đổi.

Các consumer mới cần chiến lược bootstrap/rebuild dữ liệu cũ. Event đã ở trạng thái
`DONE` sẽ không tự chạy lại khi một consumer mới được thêm sau này.

## 5. Giai đoạn D – Today Plan

### D1. Candidate và scoring

- Nguồn: SRS due, Roadmap next, weak phoneme, weak speaking, assignment due, class syllabus.
- Score: due urgency, weakness, roadmap relevance, goal relevance và freshness.
- Lọc nội dung không hợp lệ/đã hoàn thành, đảm bảo đa dạng kỹ năng.
- Ngân sách mặc định lấy từ `dailyStudyMinutes`, giới hạn 5–120 phút.

### D2. Persisted daily plan

Để Flutter có trạng thái hoàn thành và tiến độ ổn định, lưu snapshot kế hoạch ngày:

- `today_plans`: student/date/timezone/revision/profileVersion/rulesVersion/budget.
- `today_plan_items`: stable recommendation ID, target, status, estimate, reason.
- Khi profile thay đổi: giữ item `IN_PROGRESS`/`COMPLETED`, chỉ tính lại item `TODO`.
- Completion đến từ learning event của hoạt động nguồn, không có endpoint generic cho
  client tự đánh dấu hoàn thành.

### D3. API

`GET /api/v1/recommendations/today?budgetMinutes=20` trả:

- `date`, `timezone`, `revision`, `profileVersion`, `rulesVersion`;
- `budgetMinutes`, `estimatedMinutes`, `completedMinutes`, `progressPercent`;
- activity gồm target typed, status, reason code và navigation payload.

Flutter contract và quy ước refresh: `docs/module_5/today_plan_flutter_contract.md`.

## 6. Production baseline

Learning Event dùng after-commit wake-up cho dữ liệu mới; polling chỉ phục hồi retry/job
treo. Với DB giới hạn tài nguyên:

```text
ADAPTIVE_WORKER_POLL_MS=300000
ADAPTIVE_WORKER_BATCH_SIZE=50
ADAPTIVE_WORKER_MAX_ATTEMPTS=5
ADAPTIVE_WORKER_STUCK_AFTER_MS=600000
ADAPTIVE_WORKER_MAX_BACKOFF_SECONDS=300
```

Theo dõi số event `PENDING`/`FAILED`, event latency và thời gian tính Roadmap/Today Plan.
