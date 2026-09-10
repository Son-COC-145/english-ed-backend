# Module 3 — Checklist triển khai phase 4, 5 và 6

Tài liệu này là điểm tiếp tục cho phiên sau. Phase 1–3 đã xây nền quyền truy cập,
lưu turn vào DB, worker/job, report và retry. Khi bắt đầu phiên mới cần kiểm tra
`git diff`, biên dịch lại và xác nhận migration V33–V35 trước khi sửa tiếp.

## Phase 4 — Scenario, hint, greeting và bonus (P2)

### Backend

1. Kiểm kê migration V34 và dữ liệu thực tế:
   - Có ít nhất 30 scenario active thuộc A2, B1, B2, C1.
   - Mỗi scenario có context, goal, persona, avatar, system prompt hợp lệ.
   - Mỗi scenario có 3–5 hint không trùng, không rỗng.
   - Kiểm tra encoding tiếng Việt trong SQL seed.
2. Hoàn thiện API scenario:
   - Học viên chỉ thấy scenario active.
   - ADMIN/TEACHER được lọc cả active/inactive.
   - Filter `cefrLevel` có index/query test.
   - Validate goal, prompt, persona, avatar và hint ở create/update.
3. Hoàn thiện snapshot scenario lúc start:
   - Lưu goal, CEFR, prompt, context, persona và hints vào session.
   - Report luôn dùng snapshot, không đọc scenario hiện tại để chấm phiên cũ.
4. Hoàn thiện hint:
   - `POST /{sessionId}/hints` yêu cầu owner và idempotency key.
   - Cùng key trả kết quả cũ; khác key chỉ tăng một lần.
   - Lưu key đã dùng và `hint_used_count` trong transaction.
   - Không đưa toàn bộ hint vào response hội thoại thông thường.
5. Hoàn thiện greeting:
   - Tạo lượt AI greeting ngay khi start.
   - Lưu text, audio, `audio_status` (`PENDING`, `READY`, `FAILED`).
   - TTS lỗi có thể retry riêng, không tạo greeting thứ hai.
6. Hoàn thiện bonus:
   - Chỉ tính sau khi session `COMPLETED` và report hợp lệ.
   - Công thức lấy hint count từ DB, chặn kết quả âm bằng `max(0, bonus)`.
   - Thêm unique constraint/outbox hoặc điều kiện update để cấp XP đúng một lần.
   - Retry report không được cộng lại XP.

### Migration cần làm

- Bổ sung index `speaking_scenarios(cefr_level, is_active)` nếu query chưa có.
- Nếu cần log hint chi tiết, tạo `speaking_hint_usages(session_id, request_key,
  hint_index, created_at)` với unique `(session_id, request_key)`.
- Nếu greeting cần trạng thái riêng, thêm `audio_status` và `audio_error_code` vào
  `speaking_turns`; không sửa migration đã chạy, tạo V36 mới.
- Nếu XP đang ghi trực tiếp `student_stats`, bổ sung bảng reward ledger unique theo
  `session_id` trước khi bật production.

### Test nghiệm thu

- Seed có đúng tối thiểu 30 scenario và 3–5 hint/scenario.
- Học viên không đọc được prompt/hint của scenario inactive.
- Retry cùng idempotency key không tăng hint count.
- Hai request hint đồng thời chỉ tạo một usage.
- TTS greeting retry không tạo lượt AI trùng.
- `/end` hoặc worker retry nhiều lần chỉ cấp XP một lần.

## Phase 5 — Đánh giá audio và xử lý nền (P2)

### Backend

1. Chuẩn hóa audio metadata trên từng student turn:
   - MIME, kích thước, duration, thời điểm bắt đầu/kết thúc.
   - `audio_analysis_status`: `PENDING`, `MEASURED`, `INSUFFICIENT_DATA`, `FAILED`.
   - Lưu `audio_metrics_json` versioned (`metricsVersion`).
2. Đo feature:
   - word count và WPM từ transcript đã xác định.
   - pause count/pause seconds từ waveform hoặc provider timestamps.
   - filler `um`, `uh`, `erm`, `er`; không coi `like`, `well` là filler chỉ dựa vào từ.
   - lưu nguồn đo (`STT_PROVIDER`, `DECODED_AUDIO`, `UNAVAILABLE`).
3. Tách feature khỏi score:
   - Không trả 100 khi thiếu duration.
   - Không trả intonation score nếu chưa có rubric audio hiệu chỉnh.
   - Trả `scoreStatus` và `measurementStatus` rõ ràng.
4. Xây rubric fluency theo CEFR:
   - cấu hình ngưỡng WPM, filler rate, pause rate theo A2/B1/B2/C1.
   - version rubric và lưu version trong report.
   - chuẩn bị bộ mẫu đã gán nhãn để hiệu chỉnh; chưa dùng score trước khi review.
5. Job nền:
   - `/end` trả `202` và trạng thái `EVALUATING`.
   - worker xử lý audio/report độc lập với SSE.
   - retry provider theo job/turn, giữ nguyên transcript và text LLM đã lưu.
   - lease hết hạn được claim lại; worker cũ không được ghi kết quả.
6. Report progress:
   - trả trạng thái từng job/turn và lỗi gần nhất.
   - không hiển thị phần trăm giả.
   - lỗi audio không chặn hội thoại hoặc report grammar/task nếu policy cho phép.

### Migration cần làm

- Tạo V36 cho các cột audio metadata còn thiếu: `recorded_at`, `duration_seconds`,
  `audio_analysis_status`, `metrics_version`.
- Thêm bảng `speaking_rubric_versions` và `speaking_rubric_samples` nếu rubric
  được quản trị trong DB.
- Thêm index job theo `(status, available_at)` và index turn theo
  `(session_id, evaluation_status)` nếu explain plan cho thấy cần.
- Không lưu audio hoặc transcript trong application log.

### Test nghiệm thu

- Audio WAV/WebM/Ogg hợp lệ đo được duration; file hỏng chuyển `FAILED`.
- Transcript có “like”/“well” không bị đếm filler tự động.
- Thiếu duration trả `INSUFFICIENT_DATA`, score null.
- TTS/ STT timeout retry đúng job, không gọi lại công đoạn đã thành công.
- Worker chết sau provider call: lease recovery không ghi trùng turn/report.
- Report cuối cùng có số đo và nguồn đo, không có intonation score thiếu căn cứ.

## Phase 6 — Frontend integration, observability và release (P2)

### Frontend contract

1. Start:
   - xử lý `202`, lưu `sessionId`, `greetingTurnId`, trạng thái audio greeting.
2. Input:
   - gửi `Idempotency-Key` ổn định cho mỗi lần bấm gửi.
   - không tạo key mới khi retry cùng file.
   - hiển thị `PENDING`, `COMPLETED`, `FAILED` theo `turnId`.
3. SSE:
   - reconnect chỉ đọc snapshot, không gọi endpoint tạo AI.
   - upsert theo `turnId`; không nối chunk trùng.
   - dedupe audio theo `turnId`.
4. Hint panel:
   - gửi key riêng cho mỗi lần dùng hint.
   - hiển thị count server trả về, không tự tăng khi request pending.
5. End/report:
   - `202` chuyển UI sang polling report.
   - hiển thị `EVALUATING`, `COMPLETED`, `EVALUATION_FAILED`.
   - cho retry toàn phiên hoặc retry từng lượt khi được phép.
   - hiển thị score null là “chưa đủ dữ liệu”, không đổi thành 0/100.

### Backend observability

- Log có `traceId`, `jobId`, `sessionId`, `turnId`, `kind`, `attempt`, duration và
  error code; không log audio, transcript, API key hoặc provider payload.
- Metric: job latency, retry count, lease timeout, provider error, report failure,
  duplicate idempotency, XP duplicate prevented.
- Alert khi backlog job tăng, tỷ lệ `EVALUATION_FAILED` vượt ngưỡng hoặc lease timeout
  liên tục.
- Health check provider chỉ kiểm tra cấu hình/kết nối an toàn, không gửi transcript.

### Release checklist

1. Chạy compile và unit test; PostgreSQL integration test dùng DB tạm/schema riêng.
2. Chạy migration trên bản clone dữ liệu production; kiểm tra duplicate trước unique index.
3. Backup DB và xác nhận rollback procedure cho V36.
4. Chạy smoke test: start → input → SSE → end → report → retry.
5. Canary theo feature flag: worker audio, rubric score, bonus reward.
6. Theo dõi log/metric trong một chu kỳ đầy đủ trước khi bật 100%.
7. Sau release kiểm tra không có turn/job/reward trùng và report cũ vẫn đọc được.

## Thứ tự tiếp tục ở phiên sau

1. Kiểm tra compile hiện tại và sửa test còn đỏ; đặc biệt `SpeakingStorePostgresTest`
   cần gọi đúng API hiện có (`history`, không dùng `views`).
2. Rà soát các file đã xóa để chắc chắn không còn import tới STT/DTO cũ.
3. Kiểm tra các import trong main; mọi type phải nằm ở phần import đầu file.
4. Tạo V36 cho audio metadata/reward ledger nếu schema hiện tại chưa đáp ứng.
5. Triển khai lần lượt Phase 4, rồi Phase 5, rồi Phase 6; mỗi phase phải có migration,
   test và cập nhật API docs trước khi chuyển phase.
