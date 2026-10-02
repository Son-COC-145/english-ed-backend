# Flutter handoff: Onboarding và Placement Roadmap

Tài liệu này là contract ngắn gọn để Flutter tích hợp. Đặc tả đầy đủ nằm ở [flow.md](./flow.md).

## 1. Điều hướng sau đăng nhập

`AuthResponse` chỉ trả cờ tổng hợp `onboardingCompleted`; không trả `nextStep`.

- `onboardingCompleted == true`: vào Home.
- `onboardingCompleted == false`: gọi `GET /api/v1/onboarding/status` và điều hướng theo `data.nextStep`.
- `onboardingCompleted == null`: onboarding không áp dụng cho role này.

Không tự suy luận bước tiếp theo từ nhiều cờ ở client. `nextStep` của backend là nguồn quyết định duy nhất:

| `nextStep` | Xử lý ở Flutter |
|---|---|
| `GOAL_SURVEY` | Hiển thị khảo sát mục tiêu |
| `PLACEMENT_TEST` | Start/resume placement test |
| `ROADMAP_GENERATING` | Hiển thị màn hình chờ và poll roadmap status |
| `ROADMAP_FAILED` | Hiển thị retry, gọi roadmap retry khi người dùng xác nhận |
| `SETTINGS` | Hiển thị chọn XP và giờ nhắc |
| `COMPLETE` | Gọi complete khi người dùng xác nhận |
| `COMPLETED` | Vào Home |

## 2. Goal Survey dùng mã cố định

`POST /api/v1/onboarding/goal-survey`

```json
{
  "learningGoal": "COMMUNICATION",
  "otherGoalText": null,
  "focusSkills": ["SPEAKING", "PRONUNCIATION"],
  "dailyStudyMinutes": 30,
  "preferredEnvironment": "ONLINE",
  "previousExperience": "BEGINNER"
}
```

- `learningGoal` bắt buộc: `COMMUNICATION`, `WORK`, `TRAVEL`, `EXAM`, `GENERAL`.
- `focusSkills` bắt buộc, chọn 1–3 mã: `VOCABULARY`, `SPEAKING`, `PRONUNCIATION`, `READING`, `LISTENING`, `GRAMMAR`.
- `dailyStudyMinutes` tuỳ chọn, nếu gửi phải từ 5–120.
- `otherGoalText` tuỳ chọn, tối đa 200 ký tự.
- Không gửi label tiếng Việt hoặc chuỗi tự do vào `learningGoal`/`focusSkills`. Flutter tự map enum sang label hiển thị.
- Retry cùng payload là an toàn. Sau khi placement đã bắt đầu, không cho đổi payload (`5023`).

## 3. Placement Test: đúng 20 câu

Start/resume bằng `POST /api/v1/onboarding/placement-test/start`. Backend trả luôn câu đầu hoặc câu hiện tại. Không gọi endpoint `next-question` trong tích hợp mới.

Với câu không phải phát âm, gọi `POST /api/v1/onboarding/placement-test/submit-answer`:

```json
{
  "submissionId": "47a18f74-1b55-45fe-bca2-7cb9b7fdcf83",
  "sessionId": 108,
  "questionId": 25,
  "answerGiven": "B",
  "timeSpentMs": 12400
}
```

Flutter tạo một UUID mới khi render mỗi câu và giữ nguyên UUID đó cho mọi retry của cùng câu trả lời. Không tạo UUID mới khi timeout/retry mạng.

- Nếu `isTestCompleted == false`: render `nextQuestion`.
- Nếu `isTestCompleted == true`: `placementResult` có dữ liệu, dừng submit và chuyển sang trạng thái roadmap.
- Bài test chỉ hoàn tất sau câu thứ 20; không có early-stop.
- `previousCorrectAnswer` là field deprecated và luôn `null`; không phụ thuộc vào field này.

### Câu phát âm

`POST /api/v1/onboarding/placement-test/pronunciation/submit-answer`, `multipart/form-data`:

- `sessionId`
- `questionId`
- `submissionId`
- `audioFile`

Không gửi `word` hoặc reference text. Backend lấy nội dung chuẩn từ `questionId`. `INVALID_AUDIO`/dịch vụ chấm tạm unavailable không được tính là đã trả lời; giữ nguyên UUID khi retry cùng audio và tạo UUID mới khi người dùng thu âm bản mới.

## 4. Roadmap polling

Sau placement/skip, roadmap được sinh bất đồng bộ.

1. Gọi `GET /api/v1/onboarding/roadmap/status` khi đang ở màn hình chờ.
2. Nếu `PENDING`/`PROCESSING`, poll lại theo `data.retryAfterMs`; nếu thiếu thì dùng 1500–2000 ms và backoff tối đa khoảng 10 giây.
3. Dừng poll khi rời màn hình, app background, `READY` hoặc `FAILED`.
4. `READY`: gọi `GET /api/v1/onboarding/roadmap` rồi đi tiếp Settings.
5. `FAILED`: chỉ gọi `POST /api/v1/onboarding/roadmap/retry` khi người dùng bấm thử lại; response chuyển về `PENDING` và worker được đánh thức ngay.

Không poll toàn app hoặc chạy timer vĩnh viễn. Backend có durable scheduler dự phòng nên client không cần giữ worker sống.

## 5. Settings và Complete

`POST /api/v1/onboarding/settings` chỉ hợp lệ sau khi roadmap `READY`:

```json
{
  "dailyGoalXp": 20,
  "reminderTime": "20:30:00"
}
```

`dailyGoalXp` chỉ nhận `10`, `20`, `30`, `50`. `reminderTime` có thể bỏ qua.

`POST /api/v1/onboarding/complete` là idempotent. Nếu response đầu tiên bị mất, Flutter có thể retry cùng request; backend vẫn trả thành công và đồng bộ cờ tổng hợp trong `users`.

## 6. Các lỗi Flutter cần xử lý

| Code | HTTP | Ý nghĩa / xử lý |
|---:|---:|---|
| `4000` / `4001` | 400 | Payload hoặc enum sai; không retry tự động |
| `5012` | 400 | Audio rỗng/không hợp lệ; yêu cầu thu âm lại |
| `5013` | 503 | Dịch vụ phát âm tạm unavailable; không tăng câu, cho phép retry |
| `5016` | 409 | Câu hỏi không còn khớp session; gọi status/start để resync |
| `5017` | 409 | Chưa đủ 20 câu; tiếp tục bài test |
| `5018` | 409 | Roadmap đang sinh; tiếp tục polling có backoff |
| `5019` | 503 | Roadmap thất bại; hiển thị nút retry |
| `5020` | 409 | UUID đã dùng cho payload khác; lỗi quản lý idempotency ở client |
| `5021` | 409 | Submission đang xử lý; retry cùng UUID sau một khoảng ngắn |
| `5022` | 409 | Phải hoàn thành Goal Survey trước placement |
| `5023` | 409 | Goal Survey đã khoá sau khi placement bắt đầu |

## 7. Checklist smoke test sau deploy

- User mới: login → `GOAL_SURVEY`.
- Submit enum hợp lệ → `PLACEMENT_TEST`; enum lạ trả 400.
- Start hai lần trả cùng session đang hoạt động.
- Retry submit cùng UUID không tăng `questionIndex` hai lần.
- Câu 1–19 trả `isTestCompleted=false`; câu 20 trả `true` và placement result.
- Status chuyển `ROADMAP_GENERATING` → `SETTINGS` khi roadmap `READY`.
- Roadmap không có nội dung không được đánh dấu `READY`.
- Settings trước `READY` bị từ chối; sau `READY` lưu được.
- Complete gọi hai lần đều thành công; login/refresh sau đó trả `onboardingCompleted=true`.
