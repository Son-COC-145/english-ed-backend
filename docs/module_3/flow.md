# Module 3 — Speaking: phase 1–3

## Quyền và vòng đời

Mọi API theo session kiểm tra đăng nhập và chủ sở hữu, kể cả SSE, audio, hint,
report và retry. Session không tồn tại hoặc thuộc người khác đều trả 404.
Học viên chỉ đọc scenario active. ADMIN/TEACHER quản lý được scenario inactive,
nhưng không tài khoản nào bắt đầu phiên mới với scenario inactive.

Trạng thái: ONGOING → EVALUATING → COMPLETED; lỗi đánh giá chuyển sang
EVALUATION_FAILED; retry đưa về EVALUATING.

/end khóa session rồi đóng nhận lượt mới và tạo job trong cùng transaction.
Các lượt đã nhận tiếp tục hoàn tất; report đợi hội thoại và đánh giá từng lượt.
Phiên chưa có lượt học viên trả 409. End lặp lại trả trạng thái/kết quả đã có,
không tạo job hoặc cấp XP thêm. End không tự retry phiên lỗi.

## Lưu hội thoại và SSE

PostgreSQL là nguồn dữ liệu chính; luồng Speaking không phụ thuộc Redis.
Start lưu snapshot prompt, persona, context, goal, CEFR, hints và tạo lượt chào AI.
Audio/text được lưu vào speaking_turns cùng job INPUT trước khi trả 202.

- Idempotency-Key: 8–100 ký tự [A-Za-z0-9_-].
- Cùng key và SHA-256 nội dung: trả lượt cũ, kể cả sau khi end.
- Cùng key, khác payload: 409. Lượt trước đang xử lý hoặc lỗi chưa retry: 409.
- Khóa session và unique (session_id,turn_index) bảo vệ thứ tự.
- Unique request key và job theo turn/kind hoặc session/kind chống ghi trùng.
- Audio tối đa 5 MiB, phát hiện định dạng từ bytes; text tối đa 4.000 ký tự.

Worker INPUT gọi STT, lưu transcript, tạo RESPONSE và TURN_EVALUATION.
Worker RESPONSE gọi LLM, lưu text hoàn tất với response_text_ready=true, rồi gọi TTS.
TTS lỗi chỉ chạy lại TTS. Provider luôn được gọi ngoài transaction; ghi DB dùng
transaction ngắn. Audio nằm trong DB, tải qua API kiểm tra owner.

SSE chỉ đọc DB, không tạo job/gọi AI. Event transcript/text chứa snapshot toàn lượt:
frontend thay nội dung theo id, không nối vào bản cũ. Event audio có turnId/audioUrl;
frontend chống phát lại theo turnId. Reconnect nhận lại snapshot DB. Text LLM dở
có thể bị thay thế khi retry. Stream kết thúc với done khi lượt AI cuối hoàn tất,
error khi lượt thất bại; hết timeout thì reconnect nếu còn cần theo dõi.

## Scheduler, retry và log

SpeakingWorker poll theo speaking.worker.poll-ms (mặc định 500 ms), tối đa 4 worker
mỗi instance. Claim dùng FOR UPDATE SKIP LOCKED. Lease 5 phút; token cũ hoặc lease
hết hạn không được ghi kết quả. Job report chưa đủ đầu vào được bỏ qua khi poll.

Loại job: INPUT, RESPONSE, TURN_EVALUATION, SESSION_EVALUATION.
Trạng thái job: PENDING, RUNNING, COMPLETED, FAILED.
Mỗi đợt tối đa 3 lần thử; backoff bắt đầu 10, 20 giây, giới hạn 300 giây.
Retry thủ công giữ tổng attempts và cấp ngân sách thêm 3 lần.
Worker chết: job hết lease được nhận lại; hết ngân sách thì ghi lỗi mà không gọi
provider thêm. Không đảm bảo provider nhận đúng một request khi timeout xảy ra
sau khi provider đã xử lý; DB chỉ nhận kết quả từ lease hợp lệ.

| Bảng | Thuộc tính |
|---|---|
| speaking_jobs | id, session_id, turn_id, kind, status, attempts, max_attempts, available_at, lease_token, lease_expires_at, created_at, updated_at, started_at, finished_at, error_code, error_message |
| speaking_job_attempts | id, job_id, attempt_no, status, lease_token, started_at, finished_at, error_code, error_message, retryable |

Attempt có trạng thái RUNNING, SUCCEEDED, FAILED, TIMED_OUT, DEFERRED và unique
(job_id,attempt_no). Log ứng dụng ghi jobId/sessionId/kind/attempt/loại exception.
Không ghi transcript, audio, API key hay payload provider. error_message hiện lưu
tên lớp exception để tránh lộ dữ liệu; error_code là mã lỗi xử lý.

## Báo cáo và chấm điểm

Grammar DTO: original, correction, explanation.
Vocabulary DTO: original, suggestion, reason.
Không có lỗi trả mảng rỗng. Backend kiểm tra kiểu dữ liệu, nguồn trích dẫn tồn tại
trong transcript và không nhận bản sửa giống bản gốc. Legacy error/word được ánh xạ
sang original; lý do không tồn tại ở legacy trả chuỗi rỗng.

Goal snapshot tách theo dấu chấm phẩy/xuống dòng thành checklist. AI phải trả đủ
criterion theo goal_index, achieved boolean và explanation. Mục tiêu đạt cần
turn_id học viên hợp lệ và evidence đúng nguyên văn; chưa đạt có turn_id=null,
evidence="". Backend tính task_completion_score từ tỷ lệ đạt, luôn trong 0–100.

Fluency/intonation score trả null ở phase 1–3, kể cả điểm legacy chưa xác minh.
Thiếu thời lượng: fluency.scoreStatus=INSUFFICIENT_DATA. Có đo nhưng chưa hiệu chỉnh:
RUBRIC_NOT_CALIBRATED. Intonation: NOT_ASSESSED. WPM/pause/filler là số đo, chưa phải
điểm. Rubric audio được hiệu chỉnh thuộc phase sau.

Report có sessionId, status, turns (transcript, sửa lỗi, audio và trạng thái),
evaluation (criteria, general_feedback, fluency, intonationStatus), jobs (trạng thái,
attempts, lịch retry, error_code), xpEarned, hintUsedCount. Không trả phần trăm giả.
XP hiện có được ghi cùng transaction hoàn tất session; khóa và trạng thái COMPLETED
ngăn cấp trùng. Chính sách hint/bonus thuộc phase 4.

## API

Prefix: /api/v1/speaking-session. JSON được bọc trong ApiResponse.data.

| Method/path | Input | Kết quả |
|---|---|---|
| POST /start | {scenarioId} | 202, session/greetingTurnId |
| POST /{id}/audio-input | multipart file, Idempotency-Key | 202, turnId/status/transcript |
| POST /{id}/text-input | {transcript}, Idempotency-Key | 202, turnId/status/transcript |
| GET /{id}/stream-response | JWT | SSE snapshot, không gọi AI |
| POST /{id}/end | Không cần body | 202 khi xử lý/lỗi; 200 khi COMPLETED |
| GET /{id}/report | JWT | 200, trạng thái/kết quả |
| POST /{id}/retry | JWT | 202, retry job lỗi |
| POST /{id}/turns/{turnId}/retry | JWT | 202, retry lượt và report phụ thuộc |
| GET /{id}/turns/{turnId}/audio | JWT | Audio, Cache-Control: no-store |
| POST /{id}/hints | Idempotency-Key | Hints snapshot, đếm phía server |

Frontend cần tích hợp key, snapshot SSE, polling report và các trạng thái mới.
Thu âm, panel hint và transcript tăng dần từ mic cần phối hợp frontend.

## Migration và kiểm thử

V33 thêm dữ liệu bền vững/snapshot; V34 là thư viện scenario đã có; V35 thêm audit,
lease và ràng buộc. V35 giữ lượt legacy, đánh lại ordinal theo (turn_index,id) rồi
thêm unique index. Nếu có job trùng, migration dừng để xử lý dữ liệu thay vì tự xóa.
Không sửa migration đã triển khai. Hội thoại chỉ còn ở Redis của phiên cũ không tự
khôi phục từ DB: kết thúc phiên cũ trước rollout hoặc chuyển dữ liệu riêng.

Unit test kiểm tra quyền, idempotency, schema, retry TTS, lease và thiếu dữ liệu.
SpeakingStorePostgresTest opt-in trên PostgreSQL tạm qua biến
SPEAKING_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:55439/postgres, user phase123.
Test tạo/xóa schema riêng, không dùng cấu hình DB ứng dụng. Provider test dùng
HTTP server giả trên localhost, không gọi AI thật.
