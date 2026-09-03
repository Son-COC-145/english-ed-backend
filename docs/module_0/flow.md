# Kiến trúc Luồng Xử Lý (Flow Architecture) - Module 0

*Tài liệu mô tả ĐẦY ĐỦ các luồng nghiệp vụ chuẩn và Đặc tả API (API Specification) chi tiết nhất cho hệ thống Onboarding, Khảo sát mục tiêu, Bài kiểm tra phân loại thích ứng (Adaptive CAT Placement Test) và Sinh lộ trình học tập (Roadmap).*

---

## 1. Luồng Quản trị Ngân hàng Câu hỏi Phân loại (Admin Flow)
Admin quản lý ngân hàng câu hỏi trắc nghiệm và phát âm, phân loại theo độ khó CEFR (A1 - C2), kỹ năng và kích hoạt/vô hiệu hóa câu hỏi.

```mermaid
sequenceDiagram
    actor Admin
    participant System as Backend (AdminQuestionController)
    participant DB as PostgreSQL Database

    %% CRUD Ngân hàng câu hỏi
    Admin->>System: GET /api/v1/admin/questions (lọc theo CEFR, Skill, Type, Active)
    System->>DB: Truy vấn danh sách câu hỏi
    System-->>Admin: Danh sách phân trang PageResponse<AdminQuestionResponse>

    Admin->>System: POST /api/v1/admin/questions (Tạo mới câu hỏi)
    System->>DB: Validate JSON format & Lưu câu hỏi
    System-->>Admin: 201 Created (Thông tin câu hỏi)

    Admin->>System: PATCH /api/v1/admin/questions/{id}/toggle-status
    System->>DB: Cập nhật is_active = !is_active
    System-->>Admin: Trạng thái mới

    Admin->>System: GET /api/v1/admin/questions/stats
    System->>DB: Aggregate thống kê theo Skill & CEFR
    System-->>Admin: Thống kê chi tiết số lượng câu hỏi
```

### 📦 Đặc tả API tương ứng (AdminQuestionController)

#### 1.1 Quản trị Ngân hàng câu hỏi
- **`GET /api/v1/admin/questions`**: Lấy danh sách câu hỏi có phân trang & bộ lọc.
  - **Query Params:** `cefrLevel` (A1..C2), `skill` (VOCABULARY, GRAMMAR, READING, LISTENING, PRONUNCIATION), `questionType` (MULTIPLE_CHOICE, FILL_IN_BLANK, AUDIO_CHOICE, PRONUNCIATION), `isActive` (boolean), `page`, `size`.
- **`POST /api/v1/admin/questions`**: Tạo mới câu hỏi.
  - **Body (JSON):**
    ```json
    {
      "cefrLevel": "A2",
      "skill": "PRONUNCIATION",
      "questionType": "PRONUNCIATION",
      "timeoutSeconds": 30,
      "correctAnswer": "schedule",
      "content": {
        "word": "schedule",
        "ipaTranscription": "/ˈskedʒ.uːl/",
        "instruction": "Đọc to từ bên dưới vào microphone"
      }
    }
    ```
- **`PUT /api/v1/admin/questions/{id}`**: Cập nhật nội dung câu hỏi.
- **`PATCH /api/v1/admin/questions/{id}/toggle-status`**: Bật/tắt trạng thái kích hoạt.
- **`GET /api/v1/admin/questions/stats`**: Thống kê số lượng câu hỏi khả dụng theo từng kỹ năng và level.

---

## 2. Luồng Vòng đời Onboarding & Ma trận Điều hướng (State Machine Flow)
Quy trình hướng dẫn học viên qua các bước bắt buộc khi mới đăng ký tài khoản. Hệ thống thực thi cơ chế State Machine bất biến (Invariant), ngăn ngừa hoàn toàn tình trạng loop màn hình.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App (Flutter)
    participant BE as Backend (OnboardingLifecycleService)
    participant DB as Database

    Student->>App: Mở App / Đăng nhập thành công
    App->>BE: GET /api/v1/onboarding/status
    BE->>DB: Kiểm tra StudentOnboarding & active session
    BE-->>App: Trả về OnboardingStatusResponse

    alt nextStep == "GOAL_SURVEY"
        App->>Student: Hiển thị Màn hình Khảo sát mục tiêu
        Student->>App: Chọn mục tiêu, thời gian học, ngành nghề
        App->>BE: POST /api/v1/onboarding/goal-survey
        BE->>DB: Lưu GoalSurvey JSON
        BE-->>App: Lưu thành công
    else nextStep == "PLACEMENT_TEST"
        App->>Student: Điều hướng vào Bài Test (hoặc Resume câu dở nếu IN_PROGRESS)
    else nextStep == "SETTINGS"
        App->>Student: Màn hình Cài đặt Mục tiêu XP/ngày & Giờ nhắc nhở
        Student->>App: Chọn mục tiêu 50 XP/ngày & 20:00 nhắc học
        App->>BE: POST /api/v1/onboarding/settings
        BE->>DB: Lưu settings, khởi tạo DailyGoal & StudentStat
        BE-->>App: Lưu thành công
    else nextStep == "COMPLETE"
        App->>Student: Hiển thị nút "Hoàn tất bắt đầu học"
        Student->>App: Nhấn "Bắt đầu ngay"
        App->>BE: POST /api/v1/onboarding/complete
        BE->>DB: Đánh dấu onboarding_completed = true
        BE-->>App: Chúc mừng hoàn thành onboarding
    else nextStep == "COMPLETED"
        App->>Student: Vào thẳng Màn hình chính (Home Dashboard)
    end
```

### 📋 Ma trận Chuyển đổi Trạng thái Bất biến (State Invariants)

| `placementTestStatus` | `goalDone` | `settingsDone` | `isCompleted` | `nextStep` trả về | `stepNumber` | Màn hình Mobile điều hướng đến |
|---|:---:|:---:|:---:|---|:---:|---|
| `NOT_STARTED` | `false` | `false` | `false` | `"GOAL_SURVEY"` | 1 | Màn hình Khảo sát mục tiêu |
| `NOT_STARTED` | `true` | `false` | `false` | `"PLACEMENT_TEST"` | 2 | Màn hình Bắt đầu làm bài Test |
| `IN_PROGRESS` | *bất kỳ* | *bất kỳ* | `false` | `"PLACEMENT_TEST"` | 2 | Màn hình Tiếp tục làm bài test dở (`activePlacementSessionId`) |
| `COMPLETED` / `SKIPPED` | `false` | `false` | `false` | `"GOAL_SURVEY"` | 1 | Màn hình Khảo sát mục tiêu |
| `COMPLETED` / `SKIPPED` | `true` | `false` | `false` | `"SETTINGS"` | 4 | Màn hình Cài đặt mục tiêu (XP & Giờ nhắc) |
| `COMPLETED` / `SKIPPED` | `true` | `true` | `false` | `"COMPLETE"` | 4 | Dialog / Nút xác nhận "Hoàn tất Onboarding" |
| `COMPLETED` / `SKIPPED` | `true` | `true` | `true` | `"COMPLETED"` | 5 | Màn hình chính (Home/Dashboard) |

### 📦 Đặc tả API tương ứng (OnboardingController)

#### 2.1 Kiểm tra trạng thái Onboarding
- **`GET /api/v1/onboarding/status`**:
  - **Headers:** `Authorization: Bearer <accessToken>`
  - **Response (200 OK):**
    ```json
    {
      "code": 1000,
      "message": "Thành công",
      "data": {
        "goalSurveyCompleted": true,
        "placementTestCompleted": false,
        "settingsCompleted": false,
        "onboardingCompleted": false,
        "placementTestStatus": "IN_PROGRESS",
        "activePlacementSessionId": 108,
        "nextStep": "PLACEMENT_TEST",
        "stepNumber": 2,
        "totalSteps": 5,
        "userName": "Phan Thanh Sơn",
        "placementCefrLevel": null,
        "dailyGoalXp": null,
        "roadmapGenerated": false
      }
    }
    ```

#### 2.2 Nộp khảo sát mục tiêu (Goal Survey)
- **`POST /api/v1/onboarding/goal-survey`**:
  - **Body (JSON):**
    ```json
    {
      "targetLevel": "B2",
      "dailyTimeCommitmentMinutes": 30,
      "learningReason": "CAREER",
      "learningStyle": "VISUAL",
      "occupation": "SOFTWARE_ENGINEER"
    }
    ```

#### 2.3 Lưu cài đặt mục tiêu (Settings)
- **`POST /api/v1/onboarding/settings`**:
  - **Body (JSON):**
    ```json
    {
      "dailyGoalXp": 50,
      "reminderTime": "20:00"
    }
    ```

#### 2.4 Hoàn thành Onboarding
- **`POST /api/v1/onboarding/complete`**:
  - **Response (200 OK):** `{"code": 1000, "message": "Chúc mừng bạn đã hoàn thành onboarding!"}`

---

## 3. Luồng Kiểm tra Phân loại Trình độ Thích ứng (Adaptive CAT Placement Test Flow)
Quy trình làm bài kiểm tra thích ứng máy tính (Computerized Adaptive Testing - CAT). Độ khó câu hỏi tự động tăng/giảm theo từng câu trả lời đúng/sai để xác định chính xác trình độ CEFR (A1 - C2) với số lượng câu tối thiểu.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend (PlacementTestService)
    participant Azure as Azure Speech Assessment
    participant Roadmap as RoadmapGenerationService
    participant DB as Database

    %% Khởi tạo hoặc Resume
    Student->>App: Bắt đầu làm bài test
    App->>BE: POST /api/v1/onboarding/placement-test/start
    BE->>DB: Tạo session mới (hoặc trả session đang IN_PROGRESS)
    BE-->>App: Trả về câu hỏi đầu tiên (Question 1)

    %% Vòng lặp làm bài (Trắc nghiệm hoặc Phát âm)
    loop Các câu hỏi kiểm tra CAT
        alt Câu hỏi Trắc nghiệm / Nghe / Đọc
            Student->>App: Chọn đáp án
            App->>BE: POST /api/v1/onboarding/placement-test/submit-answer
            BE->>DB: Lưu câu trả lời, cập nhật CAT estimate & wrong streak
            BE-->>App: Trả về kết quả câu vừa làm + Câu tiếp theo (nextQuestion)
        else Câu hỏi Phát âm (Pronunciation)
            Student->>App: Ghi âm giọng đọc
            App->>BE: POST /placement-test/pronunciation/submit-answer (Multipart audio)
            BE->>Azure: Đánh giá audio theo Reference Text
            Azure-->>BE: Điểm tổng hợp, độ chính xác, trôi chảy
            BE->>DB: Lưu câu trả lời, cập nhật CAT estimate
            BE-->>App: Trả về đồng thời pronunciationResult + nextQuestion (Single-Trip)
        end
    end

    %% Hoàn tất bài test
    Note over BE,DB: Đạt ngưỡng tự tin (Confidence >= 85%) hoặc đủ số câu quy định
    BE->>DB: Đánh dấu session is_completed = true, lưu điểm 5 kỹ năng & CEFR vào StudentOnboarding
    BE->>Roadmap: Tự động sinh Roadmap cá nhân hóa dựa trên CEFR & Goal Survey
    Roadmap->>DB: Lưu roadmap_json
    BE-->>App: Trả về PlacementResultResponse (CEFR, Radar chart 5 kỹ năng, Lời khuyên điểm yếu, Roadmap)
    App->>Student: Hiển thị màn hình Kết quả chẩn đoán năng lực
```

### 📦 Đặc tả API tương ứng (OnboardingController)

#### 3.1 Bắt đầu bài kiểm tra (Start / Resume Idempotent)
- **`POST /api/v1/onboarding/placement-test/start`**:
  - Tự động phát hiện session còn hạn để học viên tiếp tục làm dở mà không bị mất tiến trình.

#### 3.2 Nộp câu trả lời trắc nghiệm (Structured Progression)
- **`POST /api/v1/onboarding/placement-test/submit-answer`**:
  - **Body (JSON):**
    ```json
    {
      "sessionId": 108,
      "questionId": 25,
      "answerGiven": "B",
      "timeSpentMs": 12400
    }
    ```
  - **Response (200 OK - Khi còn câu tiếp theo):**
    ```json
    {
      "code": 1000,
      "data": {
        "sessionId": 108,
        "submittedQuestionId": 25,
        "sessionStatus": "IN_PROGRESS",
        "previousAnswerCorrect": true,
        "previousCorrectAnswer": "B",
        "questionId": 26,
        "questionIndex": 4,
        "totalQuestions": 30,
        "cefrLevel": "B1",
        "skill": "GRAMMAR",
        "questionType": "MULTIPLE_CHOICE",
        "timeoutSeconds": 45,
        "content": {
          "question": "If I _____ you, I would study harder.",
          "options": ["was", "were", "am", "be"]
        },
        "nextQuestion": {
          "questionId": 26,
          "questionIndex": 4,
          "totalQuestions": 30,
          "cefrLevel": "B1",
          "skill": "GRAMMAR",
          "questionType": "MULTIPLE_CHOICE",
          "timeoutSeconds": 45,
          "content": { ... }
        },
        "isTestCompleted": false,
        "placementResult": null
      }
    }
    ```

#### 3.3 Nộp câu trả lời phát âm (Single-Trip Pronunciation Progression)
- **`POST /api/v1/onboarding/placement-test/pronunciation/submit-answer`**:
  - **Content-Type:** `multipart/form-data`
  - **Params:** `sessionId` (Long), `questionId` (Long), `audioFile` (File WAV/WebM/OGG, <= 5MB), `word` (String), `wordIndex` (int).
  - **Response (200 OK):**
    ```json
    {
      "code": 1000,
      "data": {
        "sessionId": 108,
        "submittedQuestionId": 28,
        "sessionStatus": "IN_PROGRESS",
        "pronunciationResult": {
          "word": "comfortable",
          "overallScore": 86,
          "accuracyScore": 88,
          "fluencyScore": 84,
          "completenessScore": 100,
          "scoreColor": "GREEN",
          "scoreLevel": "EXCELLENT",
          "status": "SCORED"
        },
        "nextQuestion": {
          "questionId": 29,
          "questionIndex": 6,
          "totalQuestions": 30,
          "cefrLevel": "B1",
          "skill": "LISTENING",
          "questionType": "AUDIO_CHOICE"
        },
        "isTestCompleted": false,
        "placementResult": null
      }
    }
    ```

#### 3.4 Bỏ qua bài kiểm tra (Fast-Track Onboarding cho người mới bắt đầu)
- **`POST /api/v1/onboarding/placement-test/skip`**:
  - Gán ngay trình độ mặc định `A1`, điểm sàn 20/100 cho 5 kỹ năng, và tự động sinh lộ trình tuần 1.

#### 3.5 Xem kết quả bài kiểm tra
- **`GET /api/v1/onboarding/placement-test/result`**:
  - Trả về điểm số 5 kỹ năng (để render Radar chart), phân loại CEFR, danh sách lời khuyên điểm yếu cá nhân hóa bằng tiếng Việt (`diagnosticTips`).

---

## 4. Luồng Lộ trình Học tập Cá nhân hóa (Roadmap Flow)
Quy trình sinh và theo dõi tiến độ lộ trình học tập tự động sau bài kiểm tra phân loại.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend (RoadmapGenerationService)
    participant DB as Database

    Student->>App: Xem Lộ trình học tập (Roadmap)
    App->>BE: GET /api/v1/onboarding/roadmap
    BE->>DB: Đọc roadmap_json từ StudentOnboarding
    BE-->>App: Trả về cấu trúc Milestones (Tuần 1 -> Tuần 12, từng module bài học)

    Student->>App: Mở Home / Dashboard
    App->>BE: GET /api/v1/onboarding/roadmap/progress
    BE->>DB: Tính toán số tuần hoàn thành, % tiến độ
    BE-->>App: Trả về RoadmapProgressResponse (% hoàn thành, module gợi ý tiếp theo)
    App->>Student: Hiển thị thanh tiến độ tổng quan trên Home
```

### 📦 Đặc tả API tương ứng
- **`GET /api/v1/onboarding/roadmap`**: Lấy chi tiết toàn bộ các mốc tuần học (Milestones) và danh sách bài học (Modules).
- **`GET /api/v1/onboarding/roadmap/progress`**: Lấy thống kê tổng quát:
  ```json
  {
    "code": 1000,
    "data": {
      "cefrLevel": "B1",
      "totalWeeks": 12,
      "completedWeeks": 0,
      "currentWeek": 1,
      "totalModules": 48,
      "completedModules": 0,
      "percentCompleted": 0.0,
      "nextSuggestedModule": "Nguyên âm đơn /iː/ và /ɪ/"
    }
  }
  ```
