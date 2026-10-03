# Module 5 – Adaptive Learning & Progress Intelligence · Thiết kế

> Bộ tài liệu Module 5: [Phạm vi](adaptive_learning_scope.md) · **Thiết kế** (tài liệu này) · [Kế hoạch thực thi](adaptive_learning_implementation_plan.md)
> Tài liệu này mô tả **cách làm**: kiến trúc, dữ liệu, quy tắc tính toán, API. Các giá trị số (hệ số, trọng số, thời lượng) là **mặc định**, đặt trong config để chỉnh mà không sửa code.

## 1. Kiến trúc tổng quan

```text
 Ôn từ · Mini-game · Phát âm · Speaking · Placement · Chấm bài · Sinh roadmap
                        │  ghi sự kiện CÙNG transaction nghiệp vụ
                        ▼
              learning_events (PENDING)
                        │  worker: tuần tự theo học viên, mọi consumer trong MỘT transaction
                        ▼
  RoadmapProgress → LearnerModel → StudyTime → Milestones → PlanCache
                        ▼
  roadmap_module_progress · learner_profile · learner_skill_state · student_daily_activity · learner_milestones
                        ▼
  Today Plan API · Learner Profile API · Roadmap Progress API · Progress/Milestones API  ──►  Mobile
```

**Nguyên tắc thiết kế**
1. Module hiện có là nguồn dữ liệu gốc; bảng của Module 5 là dữ liệu tổng hợp, tính lại được.
2. Mỗi hoạt động học ghi **một** sự kiện, cùng transaction với nghiệp vụ; mọi tổng hợp chỉ cập nhật qua sự kiện.
3. Không dùng AI trong luồng tính toán và đề xuất.
4. Mọi phép tính theo ngày dùng `AppTimeZone.ZONE` (`Asia/Ho_Chi_Minh`).

**Package** (theo cấu trúc hiện có):

| Package | Nội dung |
|---|---|
| `entity/adaptive`, `repository/adaptive` | Entity và repository của Module 5 |
| `service/adaptive/event` | `LearningEventOutboxService`, worker, định tuyến |
| `service/adaptive/roadmap` | `RoadmapProgressService`, snapshot nội dung |
| `service/adaptive/learner` | `LearnerModelService`, quy đổi điểm |
| `service/adaptive/activity` | `StudyTimeService` |
| `service/adaptive/recommendation` | `CandidateSource` các loại, `RecommendationScorer`, `TodayPlanService` |
| `service/adaptive/progress`, `service/adaptive/milestone` | Snapshot theo ngày, mốc (sau MVP) |
| `config/AdaptiveProperties` | `@ConfigurationProperties("adaptive")` |
| `controller/LearnerController`, `controller/RecommendationController` | API |

---

## 2. Roadmap Progress

### 2.1 Roadmap hiện nay

- Sinh ở `RoadmapGenerationService.assembleMilestones`, lưu JSON vào `student_onboarding.roadmap_json` tại `RoadmapJobService.markReady`, kèm `roadmap_generation_version`.
- Mỗi tuần là một chủ đề (tối đa 5 tuần). Module: `VOCABULARY` (từ của chủ đề), `SPEAKING` (khi học viên chọn "Giao tiếp"), `IPA_PRONUNCIATION` (tuần 1, cả 44 âm — sẽ chia nhỏ, mục 2.6).
- `RoadmapProgressService` tính tiến độ thật từ snapshot nội dung và các bảng nguồn. API đọc vẫn tính lại để làm fallback khi worker bị trễ.

### 2.2 Thay đổi trong JSON roadmap

Thêm vào `RoadmapModule` (chỉ thêm trường, JSON cũ vẫn đọc được):

| Trường | Giá trị |
|---|---|
| `moduleKey` | `VOCABULARY:{topicId}` · `SPEAKING:{topicId}` · `IPA:{nhóm âm}` (ví dụ `IPA:IPA_VOWELS_BASIC`, mục 2.6) |
| `contentItemIds` | Snapshot nội dung lúc sinh: `vocabularyId` của chủ đề · `scenarioId` đúng chủ đề và CEFR · `phonemeId` của module |
| `contentVersion` | SHA-256 rút gọn của `contentItemIds` đã sắp xếp |

`itemCount` = số phần tử `contentItemIds`. Nội dung admin thêm/xoá sau không làm đổi tiến độ của roadmap đã sinh.

### 2.3 Bảng `roadmap_module_progress`

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | bigint identity | |
| `student_id` | bigint FK users | |
| `roadmap_version` | int | = `roadmap_generation_version` |
| `week_number`, `module_index` | int | Vị trí trong roadmap, để sắp xếp |
| `module_key`, `module_type` | varchar | |
| `topic_id` | smallint null | |
| `done_count`, `total_count` | int | |
| `status` | varchar | `NOT_STARTED` / `IN_PROGRESS` / `COMPLETED` |
| `completed_at`, `updated_at` | timestamp | |

`UNIQUE(student_id, roadmap_version, module_key)`; index `(student_id, roadmap_version)`.

### 2.4 Quy tắc "làm hết mới hoàn thành"

| Loại | Một mục "đã làm" khi | Truy vấn đếm |
|---|---|---|
| `VOCABULARY` | Có dòng `student_vocabulary_progress` với `last_practiced_at` khác null | Đếm `vocabulary_id ∈ contentItemIds` |
| `SPEAKING` | Có `speaking_sessions` trạng thái `COMPLETED` | Đếm `scenario_id ∈ contentItemIds` (distinct) |
| `IPA_PRONUNCIATION` (mỗi nhóm âm) | Có `pronunciation_practice_logs` của từ ví dụ thuộc âm | Đếm distinct `phoneme_id ∈ contentItemIds` qua `ipa_example_words` |

- `progressPercent = done_count × 100 / total_count`; `status = COMPLETED` khi `done_count = total_count`, `IN_PROGRESS` khi `0 < done_count`, còn lại `NOT_STARTED`.
- "Đã làm" chỉ tăng, nên `COMPLETED` không quay lại.
- **Tuần** hoàn thành khi mọi module trong tuần `COMPLETED`. Tuần đầu luôn mở; tuần sau chỉ mở khi tuần trước hoàn thành. Mọi module trong tuần hiện tại đều có thể truy cập; Today Plan chọn module nên học trước.
- Tiến độ tổng và tiến độ tuần tính theo `sum(done_count) / sum(total_count)`, không chia đều trọng số cho các module có kích thước khác nhau.
- Module có `total_count = 0` là dữ liệu roadmap không hợp lệ. Roadmap mới có module rỗng không được chuyển sang `READY`.

### 2.5 Cách tính

- Consumer `RoadmapProgressConsumer` nhận sự kiện học tập, đếm lại theo bảng 2.4 bằng các truy vấn batch và batch-upsert toàn bộ module của **phiên bản roadmap hiện tại**. Roadmap nhỏ (tối đa khoảng 5 tuần), nên cách này ưu tiên tính đúng và khả năng tự phục hồi; có thể tối ưu thành targeted update sau khi đo tải production.
- Sự kiện `ROADMAP_GENERATED` → tạo dòng cho **mọi** module của phiên bản mới và đếm ngay (học viên đã học trước đó vẫn được tính vì đếm từ bảng nguồn).
- Khi một module chuyển sang `COMPLETED`: ghi sự kiện dẫn xuất `ROADMAP_MODULE_COMPLETED`; nếu cả tuần xong: `ROADMAP_WEEK_COMPLETED`; nếu cả roadmap xong: `ROADMAP_COMPLETED`.
- Roadmap chưa có trường mới (sinh trước khi triển khai): khi đọc, service sinh `moduleKey`/`contentItemIds` từ nội dung hiện tại và ghi lại vào JSON một lần.

### 2.6 Chia nhỏ module phát âm

Module phát âm không còn là một module 44 âm mà chia theo cột `ipa_phonemes.phoneme_type` có sẵn:

| Nhóm (`moduleKey` = `IPA:{nhóm}`) | Âm | Số âm (dữ liệu hiện tại) |
|---|---|---|
| `IPA_VOWELS_BASIC` | `VOWEL_MONO` | ≈12 |
| `IPA_DIPHTHONGS` | `VOWEL_DIPH` | ≈8 |
| `IPA_CONSONANTS_1` | Nửa đầu `CONSONANT` theo `id` | ≈12 |
| `IPA_CONSONANTS_2` | Nửa sau `CONSONANT` theo `id`, cộng `SPECIAL` (nếu có) | ≈12 |

- Danh sách âm của mỗi nhóm được chụp vào `contentItemIds` lúc sinh roadmap như mọi module khác.
- Điều kiện đưa module phát âm vào roadmap giữ như hiện nay (CEFR A1/A2, hoặc học viên chọn tập trung `PRONUNCIATION`/`SPEAKING`, hoặc mục tiêu `COMMUNICATION`).
- Mỗi tuần 1 nhóm, theo thứ tự trên, bắt đầu từ tuần 1; roadmap ít hơn 4 tuần thì các nhóm còn lại dồn vào tuần cuối.
- Nhóm có hơn `adaptive.roadmap.ipa-max-per-module` (12) âm thì chia tiếp theo `id` để mỗi module tối đa 12 âm.

---

## 3. Sự kiện học tập

### 3.1 Bảng `learning_events`

| Nhóm | Cột |
|---|---|
| Định danh | `event_id` uuid PK, `seq` bigint identity, `student_id`, `event_type` varchar(50) |
| Nguồn | `source` varchar(40), `source_reference` varchar(120) |
| Nội dung | `skill` varchar(20) null, `entity_type` varchar(30) null, `entity_id` bigint null, `score` smallint null (0–100), `duration_seconds` int null, `duration_source` varchar(10) null (`MEASURED`/`ESTIMATED`), `payload` jsonb, `schema_version` smallint, `occurred_at` timestamp, `causation_event_id` uuid null |
| Xử lý | `status` varchar(12) (`PENDING`/`PROCESSING`/`DONE`/`FAILED`), `attempt_count` int, `next_retry_at` timestamp, `last_error` text, `correlation_id` varchar(64), `processing_started_at`, `processed_at`, `created_at` |

- `UNIQUE(student_id, source, source_reference, event_type)` — chống trùng.
- Index `(status, next_retry_at)` cho worker; index `(student_id, seq)`.
- Nhóm Nội dung không bao giờ bị sửa sau khi ghi.

### 3.2 Loại sự kiện và nơi phát

| `event_type` | Nơi phát (trong transaction nghiệp vụ) | `source` / `source_reference` | skill · entity · score · duration |
|---|---|---|---|
| `PLACEMENT_COMPLETED` | `PlacementTestService.completeTestInternal`, `skipTest` | `PLACEMENT_SESSION` / id phiên (skip: `SKIP:{placementCompletedAt}`) | – · – · – · – ; `payload` = CEFR + điểm 5 kỹ năng |
| `VOCAB_REVIEWED` | `GameficationService.processReviewSubmitMutation` | `VOCAB_REVIEW` / `attemptId` | VOCABULARY · VOCABULARY/vocabularyId · rating quy đổi · 10 s ước tính |
| `VOCAB_ROUND_COMPLETED` | `MinigameRoundService.completeRound` (lần đầu chuyển `COMPLETED`) | `MINIGAME_ROUND` / roundId | VOCABULARY · TOPIC/topicId · điểm lượt · `duration_seconds` đo thật |
| `PRONUNCIATION_PRACTICED` | `IpaPronunciationServiceImpl.assess` | `PRONUNCIATION_LOG` / logId | PRONUNCIATION · EXAMPLE_WORD/wordId · `overallScore` · 60 s ước tính; `payload.phonemeId` |
| `SPEAKING_SESSION_EVALUATED` | `SpeakingStore.reportDone` (khi chuyển `COMPLETED`) | `SPEAKING_SESSION` / sessionId | SPEAKING · SCENARIO/scenarioId · TB(taskCompletion, fluency, intonation) · `ended_at − started_at` đo thật |
| `ASSIGNMENT_GRADED` | `AssignmentSubmissionService.gradeSubmission` | `ASSIGNMENT_SUBMISSION` / `{submissionId}:{gradingRevision}` | theo `moduleType` · ASSIGNMENT/assignmentId · điểm giáo viên · – |
| `ROADMAP_GENERATED` | `RoadmapJobService.markReady` (khi lưu JSON) | `ROADMAP_GENERATION` / `{studentId}:{version}` | – |
| `ROADMAP_MODULE_COMPLETED` · `ROADMAP_WEEK_COMPLETED` · `ROADMAP_COMPLETED` | `RoadmapProgressConsumer` (dẫn xuất) | `ROADMAP` / `{studentId}:{version}:{moduleKey}` · `…:WEEK:{n}` · `…:ALL` | – · ROADMAP_MODULE · – · – |

- **Thời lượng**: đo thật bị cắt tối đa 15 phút/hoạt động; ước tính lấy từ config.
- Mini-game trả lời lẻ (không thuộc lượt) **không** phát sự kiện; tiến độ từ vựng của chúng được đếm lại ở sự kiện kế tiếp (đếm từ bảng nguồn).
- `LearningEventOutboxService.saveOutbox(...)` dùng `INSERT … ON CONFLICT DO NOTHING`, chạy trong transaction gọi nó (propagation `MANDATORY`), rồi phát `LearningEventCreatedEvent` để đánh thức worker sau commit.
- Mỗi điểm phát dựng `LearningEventRequest` trong `dto.request.adaptive` và gọi `LearningEventOutboxService.saveOutbox(...)` tường minh tại đúng nhánh nghiệp vụ thành công. Cách này đặc biệt rõ ràng với các sự kiện phụ thuộc chuyển trạng thái như lần đầu chuyển sang `COMPLETED`.
- `saveOutbox(...)` dùng propagation `MANDATORY`, nên thiếu transaction sẽ fail-fast. Khi chưa có lời gọi tường minh từ nghiệp vụ, hệ thống không ghi dòng `learning_events` nào.

### 3.3 Worker

1. **Đánh thức:** `@TransactionalEventListener(AFTER_COMMIT) @Async` + `@Scheduled(fixedDelay = adaptive.worker.poll-ms)`.
2. **Claim** (một lần tối đa `batch-size` sự kiện):
   ```sql
   WITH candidates AS (
     SELECT e.event_id FROM learning_events e
     WHERE e.status='PENDING' AND e.next_retry_at <= :now
       AND NOT EXISTS (
         SELECT 1 FROM learning_events processing
         WHERE processing.student_id=e.student_id AND processing.status='PROCESSING')
       AND NOT EXISTS (
         SELECT 1 FROM learning_events earlier
         WHERE earlier.student_id=e.student_id
           AND earlier.status='PENDING' AND earlier.seq < e.seq)
     ORDER BY e.seq LIMIT :batch FOR UPDATE SKIP LOCKED)
   UPDATE learning_events e
   SET status='PROCESSING', correlation_id=:cid, processing_started_at=:now
   FROM candidates WHERE e.event_id=candidates.event_id
   RETURNING e.event_id
   ```
   Mỗi học viên chỉ lấy sự kiện `seq` nhỏ nhất, và không lấy nếu học viên đang có sự kiện `PROCESSING` → xử lý tuần tự theo học viên.
3. **Xử lý:** mỗi sự kiện một transaction, chạy các consumer theo thứ tự cố định **RoadmapProgress → LearnerModel → StudyTime → Milestones → PlanCache** (chỉ consumer được định tuyến ở 3.4), rồi `status = DONE` (kiểm tra `correlation_id` khớp).
4. **Lỗi:** rollback; transaction riêng tăng `attempt_count`, `next_retry_at = now + min(300, 2^attempt) s`, ghi `last_error` (chỉ tên lớp lỗi); `attempt_count ≥ max-attempts` → `FAILED`.
5. **Job treo:** `PROCESSING` quá `stuck-after` (10 phút) → trả về `PENDING`.

### 3.4 Định tuyến

| Sự kiện | RoadmapProgress | LearnerModel | StudyTime | Milestones | PlanCache |
|---|:-:|:-:|:-:|:-:|:-:|
| `PLACEMENT_COMPLETED` | – | ✓ khởi tạo | – | – | ✓ |
| `VOCAB_REVIEWED`, `VOCAB_ROUND_COMPLETED`, `PRONUNCIATION_PRACTICED`, `SPEAKING_SESSION_EVALUATED` | ✓ | ✓ | ✓ | ✓ streak, mastery, số hoạt động | ✓ |
| `ASSIGNMENT_GRADED` | – | – (sau MVP) | – | – | ✓ |
| `ROADMAP_GENERATED` | ✓ toàn bộ | – | – | – | ✓ |
| `ROADMAP_MODULE_COMPLETED`, `ROADMAP_WEEK_COMPLETED`, `ROADMAP_COMPLETED` | ✗ | ✗ | ✗ | ✓ | ✓ |

**Chống vòng lặp:** chỉ RoadmapProgress được ghi sự kiện mới (dẫn xuất, kèm `causation_event_id`); sự kiện dẫn xuất không bao giờ tới RoadmapProgress/LearnerModel/StudyTime; Milestones gửi thông báo qua `NotificationOutboxService`, không ghi sự kiện học tập. Chuỗi tối đa một bước.

---

## 4. Learner Model

### 4.1 Bảng

**`learner_profile`** (1 dòng / học viên)

| Cột | Ghi chú |
|---|---|
| `student_id` PK | |
| `cefr_level`, `cefr_source`, `cefr_assessed_at` | Từ placement gần nhất (`cefr_source = PLACEMENT`) |
| `overall_mastery` | numeric(5,2) null |
| `profile_version` | bigint, tăng mỗi khi bất kỳ consumer cập nhật dữ liệu của học viên (dùng làm khoá cache Today Plan) |
| `total_activities` | int |
| `created_at`, `updated_at`, `version` | `version` cho `@Version` |

**`learner_skill_state`** (`UNIQUE(student_id, skill)`)

| Cột | Ghi chú |
|---|---|
| `skill` | `VOCABULARY` / `PRONUNCIATION` / `SPEAKING` / `GRAMMAR` / `READING` / `LISTENING` |
| `source` | `PLACEMENT` (chỉ có điểm placement) / `PRACTICE` (đã có kết quả luyện) |
| `mastery` | numeric(5,2) null — null khi chưa có dữ liệu |
| `recent_score` | numeric(5,2) null |
| `confidence` | numeric(4,3) 0–1 |
| `evidence_weight` | numeric(8,3) — tổng độ tin cậy đã nhận |
| `evidence_count` | int |
| `last_practiced_at`, `updated_at`, `version` | |

### 4.2 Khởi tạo (`PLACEMENT_COMPLETED`)

- `learner_profile.cefr_level` = CEFR tổng của placement.
- Với Vocabulary, Pronunciation, Grammar, Reading, Listening: nếu kỹ năng chưa có kết quả luyện (`source = PLACEMENT` hoặc chưa có dòng) → `mastery = recent_score = điểm placement`, `confidence = 0.3`, `source = PLACEMENT`. Kỹ năng đã có kết quả luyện (`PRACTICE`) chỉ đổi CEFR, không ghi đè mastery.
- Speaking: `mastery = null` đến phiên nói đầu tiên (câu hỏi O3 trong Phạm vi).

### 4.3 Quy đổi điểm quan sát (observation) và độ tin cậy

| Sự kiện | Observation (0–100) | Reliability |
|---|---|---|
| `VOCAB_REVIEWED` | AGAIN 0 · HARD 25 · FAIR 50 · GOOD 75 · EASY 100 | 0.3 |
| `VOCAB_ROUND_COMPLETED` | Điểm lượt | `min(1, totalQuestions / 10)` |
| `PRONUNCIATION_PRACTICED` | `overallScore` | 0.6 |
| `SPEAKING_SESSION_EVALUATED` | TB các điểm khác null của task completion, fluency, intonation | 1.0 |

### 4.4 Công thức

```text
alpha        = adaptive.learner.alpha                      (0.2)
w            = alpha × reliability
mastery_new  = observation                      nếu mastery_old null
             = mastery_old + w × (observation − mastery_old)
recent_new   = observation                      nếu recent_old null
             = recent_old + 0.5 × (observation − recent_old)
evidence_weight += reliability ;  evidence_count += 1
confidence   = 1 − e^(−evidence_weight / 5)
trend        = UP nếu recent − mastery ≥ 3 · DOWN nếu ≤ −3 · STABLE còn lại
overall      = Σ(mastery × confidence) / Σ(confidence)   trên các kỹ năng có mastery
```

- Sau khi áp dụng: `source = PRACTICE`, `last_practiced_at = occurred_at`; `learner_profile.profile_version += 1`, `total_activities += 1`.
- Không giảm điểm theo thời gian (đã chốt): mastery, recent_score và trend **chỉ thay đổi khi có sự kiện học mới**; không có job nào tự giảm điểm.
- Grammar/Reading/Listening không có sự kiện luyện nên giữ `source = PLACEMENT`.

---

## 5. Thời gian học

**`student_daily_activity`**: `UNIQUE(student_id, activity_date)`; cột `measured_seconds`, `estimated_seconds`, `activity_count`, `updated_at`.

- Consumer StudyTime upsert theo `occurred_at` (ngày giờ HCM): cộng `duration_seconds` vào `measured_seconds` hoặc `estimated_seconds` theo `duration_source`, `activity_count += 1`.
- Cộng dồn tổng vào `student_stats.total_study_minutes` (cột có sẵn, đang không dùng).

---

## 6. Recommendation / Today Plan

### 6.1 Nguồn đề xuất (`CandidateSource`)

| Nguồn | Điều kiện | `type` · `target` | `reasonCode` | Phút ước tính |
|---|---|---|---|---|
| Từ đến hạn ôn | Có từ `next_review_at ≤ now` | `VOCABULARY_REVIEW` · `{mode: DUE}` | `SRS_DUE` `{dueCount}` | `min(due, 20) × 10 s` |
| Module roadmap tiếp theo | Module gợi ý của tuần hiện tại chưa xong | `ROADMAP_MODULE` · `{roadmapVersion, moduleKey}` | `ROADMAP_NEXT` `{week}` | Từ vựng 5 · Nói 8 · Phát âm 5 |
| Âm yếu | Âm đã luyện, điểm TB < 60 (tối đa 3 âm yếu nhất) | `PRONUNCIATION` · `{phonemeId}` | `WEAK_PHONEME` `{phonemeId, avgScore}` | 3 |
| Kỹ năng nói yếu | Speaking có mastery < 60, hoặc `focusSkills` chứa `SPEAKING`, hoặc `learningGoal = COMMUNICATION` | `SPEAKING` · `{scenarioId}` (kịch bản thuộc chủ đề tuần hiện tại, đúng CEFR, lâu chưa làm nhất) | `WEAK_SKILL` / `GOAL_FOCUS` `{skill}` | 8 |
| Bài tập | Bài chưa nộp: đã quá hạn (P0), hạn < 24 giờ (P1), hoặc hạn trong 3 ngày (xếp theo điểm) | `ASSIGNMENT` · `{courseId, assignmentId}` | `ASSIGNMENT_OVERDUE` `{daysOverdue}` / `ASSIGNMENT_DUE` `{hoursRemaining}` | Từ vựng 5 · Phát âm 3 · Nói 8 |
| Từ mới theo lịch lớp | Chủ đề tuần hiện tại của lớp (logic Daily Mission) còn từ chưa học | `VOCABULARY_TOPIC` · `{topicId}` | `CLASS_SYLLABUS` `{courseId}` | 5 |

Không có nguồn cho Grammar/Reading/Listening (chưa có bài luyện).

### 6.2 Chấm điểm

```text
score = dueUrgency·w1 + weakness·w2 + roadmap·w3 + goal·w4 + freshness·w5
w1..w5 mặc định = 0.35, 0.25, 0.20, 0.10, 0.10
```

| Thành phần (0–1) | Cách tính |
|---|---|
| `dueUrgency` | SRS: `min(1, due / 20)`; bài tập: `1 − hoursLeft / 72`; khác: 0 |
| `weakness` | `1 − mastery / 100` của kỹ năng (mastery null → 0.5); âm yếu: `1 − avgScore / 100` |
| `roadmap` | 1 nếu thuộc tuần roadmap hiện tại, 0 nếu không |
| `goal` | 1 nếu kỹ năng nằm trong `focusSkills` của goal survey (mã cố định, mục 6.5) |
| `freshness` | `min(1, giờ kể từ lần làm gần nhất cùng đối tượng / 48)`; chưa từng làm → 1 |

### 6.3 Lọc, đa dạng hoá, ghép theo ngân sách

1. **Lọc cứng:** bỏ ứng viên không có nội dung; bỏ module roadmap đã `COMPLETED`; bỏ bài tập đã nộp; bỏ đối tượng đã làm trong 2 giờ gần nhất (trừ `SRS_DUE`).
2. **Ưu tiên cứng cho bài tập:** **P0** bài tập quá hạn chưa nộp, rồi **P1** bài tập hạn < 24 giờ (trong mỗi mức, hạn sớm hơn đứng trước). Hai mức này luôn được đưa vào kế hoạch, kể cả khi làm vượt `budgetMinutes`. Bài tập hạn xa hơn đi qua chấm điểm như mọi ứng viên khác.
3. **Đa dạng:** mỗi `type` tối đa 1 mục (riêng `PRONUNCIATION` và `ASSIGNMENT` tối đa 2); mỗi kỹ năng tối đa 2 mục; nếu có thể thì kế hoạch có ít nhất 2 kỹ năng khác nhau.
4. **Ngân sách:** sau các mục P0/P1, duyệt ứng viên theo điểm giảm dần và chỉ thêm mục nếu tổng phút vẫn ≤ `budgetMinutes`. Kết quả: `estimatedMinutes ≤ budgetMinutes`, chỉ vượt khi có bài tập P0/P1. Không có mục nào vừa ngân sách thì trả danh sách rỗng.
5. **Giữ streak:** nếu hôm nay chưa học và `current_streak ≥ 3`, mục ít phút nhất được đổi `reasonCode` thành `KEEP_STREAK` `{streak}`.
6. `budgetMinutes` mặc định = `dailyStudyMinutes` của goal survey, không có thì 15 (câu hỏi O4 trong Phạm vi); giới hạn 5–120.

`recommendationId` = hash(`ngày`, `type`, `target`) — ổn định trong ngày, dùng cho phân tích sau này.

### 6.4 Cache

- Khoá Redis `adaptive:plan:{studentId}:{profileVersion}:{yyyyMMdd}:{budget}`, TTL = min(30 phút, đến hết ngày).
- Mọi consumer đã cập nhật dữ liệu của học viên đều tăng `profile_version` → kế hoạch cũ tự hết hiệu lực. TTL 30 phút xử lý thay đổi không qua sự kiện (giáo viên giao bài mới).
- Redis lỗi → tính trực tiếp, không chặn API.

### 6.5 Goal survey có cấu trúc

`GoalSurveyRequest` đổi từ chữ tự do sang mã cố định (vẫn lưu trong `student_onboarding.goal_survey_json`):

| Trường | Kiểu | Giá trị |
|---|---|---|
| `learningGoal` | enum `LearningGoal`, bắt buộc | `COMMUNICATION` / `WORK` / `TRAVEL` / `EXAM` / `GENERAL` |
| `otherGoalText` | string ≤ 200, tuỳ chọn | Mô tả thêm khi học viên chọn "Khác" (không dùng để tính toán) |
| `focusSkills` | list enum `LearnerSkill`, 1–3 phần tử | `VOCABULARY` / `SPEAKING` / `PRONUNCIATION` / `READING` / `LISTENING` / `GRAMMAR` |
| `dailyStudyMinutes` | int 5–120, tuỳ chọn | Ngân sách mặc định của Today Plan |
| `preferredEnvironment`, `previousExperience` | giữ như hiện tại | |

- Validation bằng Bean Validation; giá trị ngoài danh sách → `400`.
- `GoalSurveyParser` và `RoadmapGenerationService` đọc mã thay vì so chuỗi tiếng Việt ("Giao tiếp" → `SPEAKING` / `COMMUNICATION`, "Phát âm" → `PRONUNCIATION`).
- Goal survey đã lưu dạng cũ (chỉ có ở môi trường dev/test): parser đọc được cả hai dạng, dạng cũ quy đổi bằng bảng chuỗi → mã trong một chỗ duy nhất và được bỏ khi app cũ ngừng hỗ trợ (câu hỏi O6 trong Phạm vi).

---

## 7. Snapshot theo ngày và Mốc (sau MVP)

### 7.1 `learner_progress_snapshot`

`UNIQUE(student_id, snapshot_date)`; cột `cefr_level`, `overall_mastery`, `skills` (jsonb: skill → mastery, confidence), `measured_seconds`, `estimated_seconds`, `activity_count`, `current_streak`, `roadmap_week`, `completed_modules`.

- Job `@Scheduled(cron = "0 55 23 * * *", zone = "Asia/Ho_Chi_Minh")` ghi snapshot cho học viên có hoạt động trong ngày; upsert `ON CONFLICT`.
- Học viên không học ngày đó → không có dòng; biểu đồ lấy giá trị gần nhất trước đó.

### 7.2 `learner_milestones`

`UNIQUE(student_id, milestone_type, milestone_key)`; cột `achieved_at`, `payload` jsonb.

| `milestone_type` | `milestone_key` | Phát hiện khi |
|---|---|---|
| `STREAK` | 7 / 14 / 30 | `student_stats.current_streak` đạt mốc sau sự kiện học tập |
| `ROADMAP_MODULE` | `{version}:{moduleKey}` | `ROADMAP_MODULE_COMPLETED` |
| `ROADMAP_WEEK` | `{version}:{week}` | `ROADMAP_WEEK_COMPLETED` |
| `ROADMAP` | `{version}` | `ROADMAP_COMPLETED` |
| `SKILL_MASTERY` | `{skill}:60` / `{skill}:80` | mastery vượt ngưỡng lần đầu |
| `ACTIVITY_COUNT` | 10 / 50 / 100 | `total_activities` đạt mốc |

- Consumer Milestones chạy sau LearnerModel trong cùng transaction nên đọc được giá trị mới.
- Mốc thuộc danh sách thông báo (câu hỏi O5 trong Phạm vi) → `NotificationOutboxService.enqueue` (loại `SYSTEM`), khoá `milestone:{studentId}:{type}:{key}`.
- "Mốc tiếp theo" cho Progress Summary: mốc chưa đạt gần nhất (theo tỉ lệ `current / target` lớn nhất).

---

## 8. API

Tất cả: `@PreAuthorize("hasRole('STUDENT')")`, học viên lấy từ token, thời gian theo giờ HCM, lỗi theo `ApiResponse` hiện có.

### 8.1 `GET /api/v1/recommendations/today?budgetMinutes=20` (MVP)

```json
{
  "generatedAt": "2026-09-29T08:00:00",
  "profileVersion": 17,
  "rulesVersion": "2026-09-29.1",
  "budgetMinutes": 20,
  "estimatedMinutes": 18,
  "activities": [
    {
      "recommendationId": "a1f3c2",
      "type": "ASSIGNMENT",
      "title": "Bài tập tuần 3",
      "estimatedMinutes": 5,
      "target": { "courseId": 8, "assignmentId": 21 },
      "reasonCode": "ASSIGNMENT_DUE",
      "reasonParams": { "hoursRemaining": 20 },
      "priority": "P1"
    },
    {
      "recommendationId": "9b77e0",
      "type": "PRONUNCIATION",
      "title": null,
      "estimatedMinutes": 3,
      "target": { "phonemeId": 17 },
      "reasonCode": "WEAK_PHONEME",
      "reasonParams": { "phonemeId": 17, "avgScore": 48 }
    }
  ]
}
```

- `type` và cấu trúc `target` theo bảng 6.1; `title` chỉ có khi hoạt động có tên nội dung thật (bài tập, chủ đề, kịch bản).
- `reasonCode`: `SRS_DUE`, `ROADMAP_NEXT`, `WEAK_PHONEME`, `WEAK_SKILL`, `GOAL_FOCUS`, `ASSIGNMENT_OVERDUE`, `ASSIGNMENT_DUE`, `CLASS_SYLLABUS`, `KEEP_STREAK`.
- `priority`: `P0` / `P1` cho bài tập quá hạn / hạn < 24 giờ, `null` với mục xếp theo điểm. `estimatedMinutes` tổng chỉ vượt `budgetMinutes` khi có mục P0/P1.
- `rulesVersion`: phiên bản bộ trọng số và luật đề xuất đang dùng (mục 9), để debug và so sánh khi đổi trọng số. Mobile tự dịch; có thể thêm `reason` (tiếng Việt) làm fallback.

### 8.2 `GET /api/v1/learner/profile` (MVP)

```json
{
  "cefr": "A2",
  "cefrSource": "PLACEMENT",
  "cefrAssessedAt": "2026-09-20T10:15:00",
  "overallMastery": 58.4,
  "profileVersion": 17,
  "skills": [
    { "skill": "VOCABULARY", "mastery": 71.2, "confidence": 0.82, "source": "PRACTICE", "trend": "UP", "lastPracticedAt": "2026-09-29T07:40:00" },
    { "skill": "SPEAKING", "mastery": null, "confidence": 0, "source": "PLACEMENT", "trend": null, "lastPracticedAt": null },
    { "skill": "READING", "mastery": 55.0, "confidence": 0.3, "source": "PLACEMENT", "trend": null, "lastPracticedAt": null }
  ]
}
```

### 8.3 Weekly Roadmap progress v2

- `GET /api/v1/onboarding/roadmap`: snapshot roadmap ổn định theo tuần; module có `moduleKey`, `contentItemIds`, `contentVersion`.
- `GET /api/v1/onboarding/roadmap/progress`: backend trả cấu trúc `week -> module`, CEFR hiện tại/mục tiêu, `currentWeek`, `currentModuleKey`, completion, accessibility, unlock condition và progress tính thật.
- Roadmap không chứa ngày học. Phân bổ theo ngày thuộc Today Plan tại `GET /api/v1/recommendations/today`.
- `moduleKey` là opaque identifier. Mobile không parse key, không match theo title và không tự tính business rule.
- `cefrLevel` và `nextSuggestedModule` chỉ giữ tạm trong cửa sổ migration; client mới dùng `currentCefrLevel` và `currentModuleKey`.
- Contract chi tiết: `docs/module_0/roadmap_progress_contract.md`.

### 8.4 Sau MVP

`GET /api/v1/learner/progress/summary`

```json
{
  "cefr": "A2",
  "overallMastery": 58.4,
  "currentRoadmap": { "roadmapVersion": 2, "week": 3, "moduleKey": "SPEAKING:12" },
  "today": { "totalStudyMinutes": 24, "measuredMinutes": 18, "estimatedMinutes": 6, "activities": 5 },
  "focusSkills": ["PRONUNCIATION", "SPEAKING"],
  "improvements": [{ "skill": "VOCABULARY", "delta": 4.2, "period": "7d" }],
  "nextMilestone": { "type": "STREAK", "current": 6, "target": 7 }
}
```

- `focusSkills` = 2 kỹ năng `PRACTICE` có mastery thấp nhất; `improvements` = chênh mastery so với snapshot 7 ngày trước.
- `GET /api/v1/learner/progress/history?range=7d|30d|90d` → danh sách snapshot.
- `GET /api/v1/learner/milestones` → `{ achieved: [...], upcoming: [...] }`.

### 8.5 Quy ước cho Mobile

- Sau khi học xong một hoạt động và quay lại, tải lại Today Plan / Profile (có thể hiện dữ liệu cache trước rồi làm mới).
- Một API lỗi chỉ thử lại phần đó, giữ dữ liệu cache, không chặn cả màn hình.
- Không tự tính mastery, không tự sắp xếp đề xuất.

---

## 9. Cấu hình (`application.yaml`)

```yaml
adaptive:
  worker:
    poll-ms: 5000
    batch-size: 50
    max-attempts: 8
    stuck-after: 10m
  study-time:
    max-seconds-per-activity: 900
    estimated-seconds:
      VOCAB_REVIEWED: 10
      PRONUNCIATION_PRACTICED: 60
  learner:
    alpha: 0.2
    placement-confidence: 0.3
    reliability:
      VOCAB_REVIEWED: 0.3
      PRONUNCIATION_PRACTICED: 0.6
      SPEAKING_SESSION_EVALUATED: 1.0
  roadmap:
    ipa-max-per-module: 12
  recommendation:
    rules-version: "2026-09-29.1"      # tăng mỗi khi đổi trọng số / luật
    default-budget-minutes: 15
    weights:                           # giá trị do Product/Learning owner quyết định
      due-urgency: 0.35
      weakness: 0.25
      roadmap: 0.20
      goal: 0.10
      freshness: 0.10
    weak-phoneme-threshold: 60
    assignment-horizon-days: 3
    cache-ttl: 30m
```

---

**Quản lý trọng số:** Backend chịu trách nhiệm cơ chế (đọc config, validate tổng trọng số = 1, log `rules-version` khi khởi động); Product/Learning owner quyết định giá trị. Mọi trọng số chỉ nằm trong khối `adaptive.recommendation.weights`, `RecommendationScorer` là nơi duy nhất đọc chúng. Đổi giá trị = sửa config + tăng `rules-version`, không sửa code.

## 10. Theo dõi và lỗi

- Metric (Micrometer/Actuator): số sự kiện theo `status`, sự kiện `FAILED`, độ trễ từ `created_at` đến `processed_at`, thời gian tạo Today Plan, tỉ lệ cache hit.
- Log: mỗi lỗi consumer ghi `event_id`, `event_type`, `student_id`, tên lớp lỗi (không ghi dữ liệu nhạy cảm).
- API Module 5 không bao giờ chờ worker: nếu sự kiện chưa xử lý xong, trả dữ liệu hiện có.

## 11. Kiểm thử

| Loại | Nội dung |
|---|---|
| Unit (JUnit 5 + Mockito) | Quy tắc làm-hết và tuần hiện tại; EMA, confidence, trend, khởi tạo placement; quy đổi observation; chấm điểm, lọc, đa dạng, ngân sách; định tuyến; phát hiện mốc |
| Tích hợp Postgres (bật bằng biến môi trường như `Module4PostgresIntegrationTest`) | Chống trùng `ON CONFLICT`; claim tuần tự theo học viên; retry và `FAILED`; job treo; roadmap sinh lại |
| Hợp đồng API | Cấu trúc JSON Today Plan/Profile đúng mẫu mục 8 |
