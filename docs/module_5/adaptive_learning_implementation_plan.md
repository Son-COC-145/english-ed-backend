# Module 5 – Adaptive Learning & Progress Intelligence · Kế hoạch thực thi

> Bộ tài liệu Module 5: [Phạm vi](adaptive_learning_scope.md) · [Thiết kế](adaptive_learning_design.md) · **Kế hoạch thực thi** (tài liệu này)
> Tài liệu này mô tả **làm theo thứ tự nào, mỗi bước giao gì, kiểm tra thế nào**. Ước lượng tính theo ngày công của 1 lập trình viên backend.

## 1. Tổng quan

| Giai đoạn | Bước | Kết quả | Ước lượng |
|---|---|---|---|
| **MVP** | 1. Tiến độ roadmap | Roadmap có mã module, snapshot nội dung, module phát âm chia nhỏ, tiến độ thật | 3,5 ngày |
| | 2. Hạ tầng sự kiện học tập | Bảng `learning_events`, publisher, worker | 3 ngày |
| | 3. Hook các module | 7 điểm phát sự kiện | 2 ngày |
| | 4. Consumer: roadmap, năng lực, thời gian học | Dữ liệu tổng hợp cập nhật theo sự kiện | 3 ngày |
| | 5. Recommendation + Today Plan API | `GET /recommendations/today` | 4 ngày |
| | 5a. Goal survey có cấu trúc | API goal survey nhận mã cố định; roadmap và đề xuất đọc mã | 1,5 ngày |
| | 6. Learner Profile API + API roadmap | `GET /learner/profile`, roadmap trả số thật | 1 ngày |
| | 7. Kiểm thử tích hợp, demo MVP | Chạy đủ kịch bản demo (Phạm vi mục 8) | 2 ngày |
| **Sau MVP** | 8. Snapshot theo ngày + Progress API | Summary, History | 2 ngày |
| | 9. Mốc + thông báo | Milestones API, thông báo đẩy | 2 ngày |
| | 10. Reading & Listening | Engine bài đọc/nghe, nội dung AI có duyệt | tách kế hoạch riêng |

**MVP ≈ 20 ngày công.** Sau MVP (bước 8–9) ≈ 4 ngày công.

```text
Bước 5a ─┐
Bước 1 ──┼─► Bước 3 ─► Bước 4 ─► Bước 5 ─► Bước 6 ─► Bước 7
Bước 2 ──┘                                   └─► Bước 8 ─► Bước 9
```

Bước 1, 2 và 5a độc lập, làm song song được. Nên làm **5a sớm** vì Mobile cần đổi màn hình khảo sát.

**Trước khi bắt đầu:** nhóm xem lại các câu hỏi mở (Phạm vi mục 6). Câu nào chưa chốt thì dùng đề xuất mặc định trong bảng đó, không chặn tiến độ.

**Migration:** số version dưới đây giả định bản mới nhất trên `main` là `V58`; khi làm, lấy số kế tiếp thực tế.

---

## 2. Chi tiết từng bước

### Bước 1 – Tiến độ roadmap (Thiết kế mục 2)

**Việc cần làm**
1. Migration `V59__roadmap_module_progress.sql`: bảng `roadmap_module_progress` (Thiết kế 2.3).
2. `RoadmapModule`: thêm `moduleKey`, `contentItemIds`, `contentVersion`.
3. `RoadmapGenerationService.assembleMilestones`: điền 3 trường trên khi sinh (từ vựng: id từ của chủ đề; nói: id kịch bản đúng chủ đề + CEFR).
   Chia module phát âm theo `phoneme_type` thành `IPA_VOWELS_BASIC`, `IPA_DIPHTHONGS`, `IPA_CONSONANTS_1`, `IPA_CONSONANTS_2` (tối đa 12 âm/module), mỗi tuần 1 nhóm (Thiết kế 2.6).
4. `RoadmapContentResolver`: roadmap cũ chưa có trường mới → sinh từ nội dung hiện tại, ghi lại JSON một lần.
5. `RoadmapProgressService`: đếm "đã làm" theo 3 loại (Thiết kế 2.4), upsert tiến độ, tính tuần hiện tại / module gợi ý. Chưa có sự kiện thì cho phép gọi trực tiếp `recalculateAll(studentId)`.
6. `OnboardingLifecycleService.getRoadmapProgress` và API `GET /onboarding/roadmap`: trả số thật và các trường mới.

**Kiểm tra**
- Unit test: đếm đúng từng loại; `total_count = 0` coi là xong; tuần hiện tại / module gợi ý; roadmap cũ không có trường mới vẫn đọc được; chia nhóm âm đúng, không module nào quá 12 âm, roadmap < 4 tuần dồn nhóm còn lại vào tuần cuối.
- Thủ công: học vài từ của chủ đề tuần 1 → gọi `recalculateAll` → API tiến độ thay đổi đúng.

### Bước 2 – Hạ tầng sự kiện học tập (Thiết kế mục 3)

**Việc cần làm**
1. Migration `V60__learning_events.sql`: bảng, `UNIQUE(student_id, source, source_reference, event_type)`, các index.
2. Entity/enum: `LearningEvent`, `LearningEventType`, `LearningEventSource`, `LearningEventStatus`, `DurationSource`.
3. `LearningEventPublisher.publish(...)`: `INSERT … ON CONFLICT DO NOTHING`, propagation `MANDATORY`, phát `LearningEventCreated`.
4. `LearningEventWorker`: đánh thức sau commit + `@Scheduled`; claim theo câu SQL ở Thiết kế 3.3 (tuần tự theo học viên, `FOR UPDATE SKIP LOCKED`); xử lý mỗi sự kiện một transaction; retry backoff; `DEAD`; trả job treo.
5. `LearningEventRouter`: bảng định tuyến (Thiết kế 3.4) + interface `LearningEventConsumer` (`supports(type)`, `apply(event)`), thứ tự consumer cố định.
6. `AdaptiveProperties` + khối `adaptive:` trong `application.yaml` (Thiết kế mục 9).

**Kiểm tra**
- Unit test: định tuyến; thứ tự consumer; tính backoff.
- Tích hợp Postgres: ghi trùng chỉ còn 1 dòng; 2 sự kiện của cùng học viên xử lý đúng thứ tự `seq`; consumer ném lỗi → retry → `DEAD`; `PROCESSING` quá hạn trở về `PENDING`; nghiệp vụ rollback → không có sự kiện.

### Bước 3 – Hook các module (Thiết kế 3.2)

| Module | Chỗ gọi `publish` | Sự kiện |
|---|---|---|
| Placement | `PlacementTestService.completeTestInternal`, `skipTest` | `PLACEMENT_COMPLETED` |
| Ôn từ | `GameficationService.processReviewSubmitMutation` | `VOCAB_REVIEWED` |
| Mini-game | `MinigameRoundService.completeRound` (chỉ lần đầu `COMPLETED`) | `VOCAB_ROUND_COMPLETED` |
| Phát âm | `IpaPronunciationServiceImpl.assess` | `PRONUNCIATION_PRACTICED` |
| Speaking | `SpeakingStore.reportDone` (khi chuyển `COMPLETED`) | `SPEAKING_SESSION_EVALUATED` |
| Chấm bài | `AssignmentSubmissionService.gradeSubmission` | `ASSIGNMENT_GRADED` |
| Sinh roadmap | `RoadmapJobService.markReady` (khi lưu JSON) | `ROADMAP_GENERATED` |

**Lưu ý:** mỗi chỗ gọi phải nằm **trong** transaction nghiệp vụ. Kiểm tra các method trên đều `@Transactional` (hoặc được gọi từ method `@Transactional`); `SpeakingStore` và `RoadmapJobService.markReady` đã có sẵn.

**Kiểm tra**
- Unit test mỗi module: gọi đúng `publish` với `source_reference`, `score`, `duration` đúng (Thiết kế 3.2).
- Unit test: gọi lại request ôn từ cùng `attemptId` / chấm lại phiên nói không tạo sự kiện thứ hai.

### Bước 4 – Consumer (Thiết kế mục 2.5, 4, 5)

**Việc cần làm**
1. `RoadmapProgressConsumer`: nối `RoadmapProgressService` vào sự kiện; phát `ROADMAP_MODULE_COMPLETED` / `ROADMAP_WEEK_COMPLETED` / `ROADMAP_COMPLETED`.
2. Migration `V61__learner_model.sql`: `learner_profile`, `learner_skill_state`.
3. `LearnerModelConsumer` + `ObservationMapper`: khởi tạo từ placement, quy đổi observation, EMA, confidence, trend, overall; tăng `profile_version`.
4. Migration `V62__student_daily_activity.sql`.
5. `StudyTimeConsumer`: upsert theo ngày giờ HCM, tách đo thật / ước tính, trần 15 phút; cộng `student_stats.total_study_minutes`.
6. `PlanCacheConsumer`: tăng `profile_version` cho sự kiện chỉ tới cache (ví dụ `ASSIGNMENT_GRADED`, `ROADMAP_GENERATED`).

**Kiểm tra**
- Unit test: bảng quy đổi observation; EMA lần đầu (null) và các lần sau; placement không ghi đè kỹ năng đã `PRACTICE`; Speaking null đến phiên đầu; confidence/trend/overall; thời gian học tách đúng loại, cắt trần.
- Tích hợp: một sự kiện lỗi ở consumer thứ 3 → không consumer nào được áp dụng; xử lý lại → EMA chỉ áp dụng một lần.

### Bước 5 – Recommendation + Today Plan API (Thiết kế mục 6, 8.1)

**Việc cần làm**
1. Interface `CandidateSource` + 6 nguồn: `SrsDueSource`, `RoadmapNextSource`, `WeakPhonemeSource`, `SpeakingSource`, `AssignmentDueSource`, `ClassSyllabusSource` (tách logic từ `getDailyMission`).
2. Nguồn `goal` đọc `focusSkills` / `learningGoal` dạng mã từ bước 5a.
3. `RecommendationScorer`: 5 thành phần điểm, trọng số từ config.
4. `TodayPlanService`: lọc cứng → bài tập P0 (quá hạn) / P1 (< 24 giờ) → đa dạng → ghép ngân sách (tổng ≤ `budgetMinutes`, chỉ P0/P1 được vượt) → `KEEP_STREAK` → `recommendationId`, `rulesVersion`.
5. Cache Redis theo `profileVersion` (Thiết kế 6.4); Redis lỗi thì tính trực tiếp.
6. `RecommendationController`: `GET /api/v1/recommendations/today`.
7. `GET /gamification/daily-mission`: đánh dấu deprecated, trả phần từ vựng lấy từ Today Plan.

**Kiểm tra**
- Unit test mỗi nguồn (có / không có ứng viên); scorer; lọc cứng; đa dạng; ngân sách (`estimatedMinutes ≤ budgetMinutes` khi không có P0/P1; có P0/P1 thì các mục đó luôn có mặt); P0 đứng trước P1; `KEEP_STREAK`; tổng trọng số khác 1 → báo lỗi khi khởi động.
- Hợp đồng JSON đúng Thiết kế 8.1.

### Bước 5a – Goal survey có cấu trúc (Thiết kế 6.5)

1. Enum `LearningGoal`, `LearnerSkill`; đổi `GoalSurveyRequest` (`learningGoal`, `otherGoalText`, `focusSkills`, `dailyStudyMinutes`) + Bean Validation.
2. `GoalSurveyParser`: đọc dạng mới; dạng cũ quy đổi chuỗi → mã ở một chỗ duy nhất.
3. `RoadmapGenerationService`: dùng mã thay cho so chuỗi "Giao tiếp" / "Phát âm".
4. Cập nhật Swagger; gửi Mobile hợp đồng mới của API goal survey.

**Kiểm tra:** unit test validation (giá trị ngoài danh sách → 400, 1–3 kỹ năng); parser đọc được cả dạng cũ và mới; roadmap sinh module Speaking/phát âm đúng theo mã.

### Bước 6 – Learner Profile API + roadmap API

1. `LearnerController`: `GET /api/v1/learner/profile` (Thiết kế 8.2).
2. Xác nhận `GET /onboarding/roadmap` và `/roadmap/progress` trả đủ trường mới (Thiết kế 8.3).
3. Cập nhật Swagger (springdoc) cho các API mới.

### Bước 7 – Kiểm thử tích hợp và demo MVP

1. Chạy đủ kịch bản demo (Phạm vi mục 8) trên môi trường dev với Postgres + Redis Docker.
2. Kiểm tra Definition of Done (Phạm vi mục 7) từng mục.
3. Kiểm tra metric trên Actuator: sự kiện `PENDING`/`DEAD`, độ trễ xử lý.
4. Gửi Mobile: tài liệu API (Thiết kế mục 8) + Swagger.

### Bước 8 – Snapshot theo ngày + Progress API (sau MVP, Thiết kế 7.1, 8.4)

1. Migration `V63__learner_progress_snapshot.sql`.
2. Job `@Scheduled` 23:55 giờ HCM ghi snapshot.
3. `GET /learner/progress/summary`, `GET /learner/progress/history`.

### Bước 9 – Mốc + thông báo (sau MVP, Thiết kế 7.2)

1. Migration `V64__learner_milestones.sql`.
2. `MilestoneConsumer`: 6 loại mốc; thông báo qua `NotificationOutboxService`.
3. `GET /learner/milestones`.

---

## 3. Chia Pull Request

| PR | Nội dung | Phụ thuộc |
|---|---|---|
| PR1 | Bước 1 (tiến độ roadmap, gọi trực tiếp) | – |
| PR2 | Bước 2 (hạ tầng sự kiện, chưa có consumer thật) | – |
| PR3 | Bước 3 + `RoadmapProgressConsumer` | PR1, PR2 |
| PR4 | Learner Model + Study Time + Plan cache consumer | PR3 |
| PR5a | Goal survey có cấu trúc | – |
| PR5 | Recommendation + Today Plan API | PR4, PR5a |
| PR6 | Learner Profile API, Swagger, Daily Mission deprecated | PR5 |
| PR7 | Snapshot + Progress API | PR6 |
| PR8 | Milestones | PR7 |

Mỗi PR: có test, chạy `mvn test` xanh, cập nhật tài liệu Thiết kế nếu có thay đổi so với thiết kế.

---

## 4. Rủi ro và cách xử lý

| Rủi ro | Ảnh hưởng | Cách xử lý |
|---|---|---|
| Method hook không nằm trong transaction | Sự kiện mất hoặc ghi khi nghiệp vụ đã rollback | Publisher dùng propagation `MANDATORY` → lỗi ngay khi test nếu thiếu transaction |
| Nhóm âm vẫn nặng với một số học viên | Tiến độ tuần chậm | Đã chia tối đa 12 âm/module; có thể giảm `ipa-max-per-module` trong config |
| Hệ số EMA / trọng số đề xuất chưa hợp lý | Đề xuất không sát nhu cầu | Mọi hệ số nằm trong config; Product/Learning owner chỉnh trọng số, tăng `rules-version` (câu hỏi O8) |
| Worker chậm khi nhiều sự kiện | Today Plan trễ vài giây | API không chờ worker; theo dõi độ trễ trên Actuator; tăng `batch-size` |
| Roadmap cũ không có snapshot nội dung | Tiến độ tính theo nội dung hiện tại | `RoadmapContentResolver` ghi snapshot một lần khi đọc; chấp nhận vì chưa có dữ liệu production |
| App mobile cũ vẫn gọi Daily Mission / gửi goal survey dạng cũ | Hai nguồn gợi ý; dữ liệu survey không đúng mã | Daily Mission trả dữ liệu lấy từ Today Plan; parser đọc được dạng cũ cho đến khi bỏ (câu hỏi O6) |

---

## 5. Checklist nghiệm thu MVP

- [ ] Migration V59–V62 chạy sạch trên DB trống và DB dev hiện có.
- [ ] 7 điểm phát sự kiện hoạt động; gửi lại request không tạo sự kiện trùng.
- [ ] Không có sự kiện nào `DEAD` khi chạy kịch bản demo.
- [ ] Tiến độ roadmap đúng; làm lại placement có roadmap mới và tiến độ mới.
- [ ] Năng lực Vocabulary / Pronunciation / Speaking đổi sau hoạt động tương ứng.
- [ ] Thời gian học hôm nay đúng giờ Việt Nam, tách đo thật / ước tính.
- [ ] Today Plan có `estimatedMinutes ≤ budgetMinutes` (chỉ vượt khi có bài tập P0/P1), P0 trước P1, có `reasonCode`, `rulesVersion`, thay đổi sau khi học.
- [ ] Goal survey nhận mã cố định; roadmap sinh module phát âm chia nhỏ (≤ 12 âm/module).
- [ ] Learner Profile đúng mẫu.
- [ ] `mvn test` xanh; test tích hợp Postgres chạy được khi bật biến môi trường.
- [ ] Swagger và tài liệu Thiết kế khớp với code.
