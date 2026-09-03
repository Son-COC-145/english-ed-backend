# TÀI LIỆU HỢP ĐỒNG API CẬP NHẬT: MODULE 0 (ONBOARDING) & MODULE 1 (IPA PRONUNCIATION)
**Dành cho:** Frontend Team (Flutter) & Backend Team  
**Phiên bản:** 2.1 - Post-Review Alignment  
**Trạng thái Backend:** Đã cập nhật 100%, pass toàn bộ test suite (`71/71 tests passed`).

---

## 📌 TỔNG QUAN CÁC ĐIỂM CẢI TIẾN THEO ĐỀ XUẤT FRONTEND

1. **Chuẩn hóa Invariant trạng thái Onboarding (`GET /api/v1/onboarding/status`)**:
   - Khắc phục triệt để lỗi kẹt ở `nextStep = "SETTINGS"` khi `settingsCompleted = true`.
   - Thêm cờ `placementTestStatus` rõ ràng: `NOT_STARTED | IN_PROGRESS | COMPLETED | SKIPPED`.
   - Bổ sung `activePlacementSessionId` khi trạng thái là `IN_PROGRESS` để Flutter resume bài test ngay lập tức mà không phải bắt user làm lại từ đầu.
   - Khi cả 3 bước (`placementTestCompleted`, `goalSurveyCompleted`, `settingsCompleted`) đều xong mà chưa bấm hoàn tất -> trả `nextStep = "COMPLETE"` (không bao giờ trả lại `"SETTINGS"`).
2. **Loại bỏ điểm nghẽn nộp Audio Placement Test (`POST /placement-test/pronunciation/submit-answer`)**:
   - Trả về đồng thời cả kết quả chấm âm thanh (`pronunciationResult`) VÀ thông tin câu tiếp theo (`nextQuestion`) hoặc kết quả hoàn thành (`placementResult`) trong **cùng 1 response**.
   - Mobile Flutter **không cần và không nên** gọi endpoint `GET /placement-test/next-question` riêng lẻ nữa.
3. **Structured Progression nhất quán (`POST /placement-test/submit-answer`)**:
   - Trả về cả dạng phẳng lẫn đối tượng lồng `nextQuestion: { ... }` và `sessionStatus: "IN_PROGRESS" | "COMPLETED"`.
   - Không bao giờ trả `data: null` gây crash màn hình trắc nghiệm.
4. **Chuẩn hóa HTTP Status Code & Mã lỗi Audio máy đọc được**:
   - `415 Unsupported Media Type` (`code: 5010` - `UNSUPPORTED_AUDIO_FORMAT`): Magic bytes không phải WAV/WEBM/OGG.
   - `413 Payload Too Large` (`code: 5011` - `AUDIO_PAYLOAD_TOO_LARGE`): File âm thanh vượt quá 5MB.
   - `400 Bad Request` (`code: 5012` - `AUDIO_EMPTY_OR_CORRUPT`): File rỗng hoặc < 4 bytes.
   - `503 Service Unavailable` (`code: 5013` - `PRONUNCIATION_UNAVAILABLE`): Azure Speech timeout/lỗi.
5. **Typed Bookmark DTO & Bổ sung cờ Bookmark vào Phoneme Detail**:
   - `POST /api/v1/ipa/phonemes/{id}/bookmark` trả về Object `{ "phonemeId": 1, "isBookmarked": true }` thay vì `Map<String, Boolean>`.
   - `GET /api/v1/ipa/phonemes/{id}` bổ sung trường `isBookmarked: boolean` của user đang đăng nhập.
6. **Mở rộng kết quả luyện âm IPA chi tiết (Phoneme-level Feedback)**:
   - Thêm `errorType`: `NONE | SUBSTITUTION | OMISSION | INSERTION | FINAL_SOUND_MISSING | STRESS`.
   - Thêm `correctionHint`, `scoreLevel`: `EXCELLENT | GOOD | NEEDS_PRACTICE | NONE`.
   - Trả về `practiceId`, `exampleWordId`, `phonemeId` để Mobile lưu cache và highlight trực quan.
7. **Validation tham số chặt chẽ cho `/tts/stream`**:
   - Trả lỗi 400 rõ ràng nếu thiếu `text`, `text > 150 ký tự`, hoặc sai `voice` / `type`.

---

## 🔄 MA TRẬN ĐIỀU HƯỚNG ONBOARDING (STATE MACHINE)

| Trạng thái Placement | Goal Done | Settings Done | Onboarding Done | `nextStep` trả về | `stepNumber` | Màn hình Mobile điều hướng đến |
|---|:---:|:---:|:---:|---|:---:|---|
| `NOT_STARTED` | `false` | `false` | `false` | `"GOAL_SURVEY"` | 1 | Màn hình Khảo sát mục tiêu |
| `NOT_STARTED` | `true` | `false` | `false` | `"PLACEMENT_TEST"` | 2 | Màn hình Bắt đầu làm bài Test |
| `IN_PROGRESS` | *bất kỳ* | *bất kỳ* | `false` | `"PLACEMENT_TEST"` | 2 | Màn hình Tiếp tục làm bài test dở (`activePlacementSessionId`) |
| `COMPLETED` / `SKIPPED` | `false` | `false` | `false` | `"GOAL_SURVEY"` | 1 | Màn hình Khảo sát mục tiêu |
| `COMPLETED` / `SKIPPED` | `true` | `false` | `false` | `"SETTINGS"` | 4 | Màn hình Cài đặt mục tiêu (XP & Giờ nhắc) |
| `COMPLETED` / `SKIPPED` | `true` | `true` | `false` | `"COMPLETE"` | 4 | Dialog / Nút xác nhận "Hoàn tất Onboarding" |
| `COMPLETED` / `SKIPPED` | `true` | `true` | `true` | `"COMPLETED"` | 5 | Màn hình chính (Home/Dashboard) |

---

## 📚 CHI TIẾT CONTRACT CÁC ENDPOINT MODULE 0: ONBOARDING

### 1. Kiểm tra trạng thái Onboarding
- **Endpoint:** `GET /api/v1/onboarding/status`
- **Headers:** `Authorization: Bearer <accessToken>`
- **Response mẫu:**
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
    "activePlacementSessionId": 105,
    "nextStep": "PLACEMENT_TEST",
    "stepNumber": 2,
    "totalSteps": 5,
    "userName": "Nguyễn Văn A",
    "placementCefrLevel": null,
    "dailyGoalXp": null,
    "roadmapGenerated": false
  }
}
```

---

### 2. Nộp khảo sát mục tiêu (Goal Survey)
- **Endpoint:** `POST /api/v1/onboarding/goal-survey`
- **Payload:**
```json
{
  "goals": ["COMMUNICATION", "CAREER"],
  "targetCefr": "B2",
  "dailyCommitmentMinutes": 20,
  "currentProficiency": "A2"
}
```
- **Response:**
```json
{
  "code": 1000,
  "message": "Lưu khảo sát mục tiêu thành công",
  "data": {
    "goalSurveyCompleted": true,
    "nextStep": "PLACEMENT_TEST",
    "stepNumber": 2
  }
}
```

---

### 3. Bắt đầu bài kiểm tra Placement Test (Idempotent)
- **Endpoint:** `POST /api/v1/onboarding/placement-test/start`
- **Lưu ý:** Nếu user đã có session `IN_PROGRESS` chưa hết hạn (30 phút), API sẽ trả về câu hỏi tiếp theo của session đó, không tạo session thừa.
- **Response mẫu:**
```json
{
  "code": 1000,
  "message": "Thành công",
  "data": {
    "sessionId": 105,
    "sessionStatus": "IN_PROGRESS",
    "questionId": 42,
    "questionIndex": 1,
    "totalQuestions": 15,
    "cefrLevel": "A2",
    "skill": "VOCABULARY",
    "questionType": "MULTIPLE_CHOICE",
    "timeoutSeconds": 45,
    "content": {
      "question": "Choose the word that best fits the blank: 'She has been working here ___ 2018.'",
      "options": ["since", "for", "in", "from"],
      "explanation": "Dùng 'since' với mốc thời gian."
    },
    "isTestCompleted": false
  }
}
```

---

### 4. Nộp câu trả lời Trắc nghiệm / Điền từ
- **Endpoint:** `POST /api/v1/onboarding/placement-test/submit-answer`
- **Payload:**
```json
{
  "sessionId": 105,
  "questionId": 42,
  "answerGiven": "since",
  "timeSpentMs": 12500
}
```
- **Response (Khi bài test chưa kết thúc):**
```json
{
  "code": 1000,
  "message": "Thành công",
  "data": {
    "sessionId": 105,
    "submittedQuestionId": 42,
    "sessionStatus": "IN_PROGRESS",
    "previousAnswerCorrect": true,
    "previousCorrectAnswer": "since",
    "isTestCompleted": false,
    "questionId": 43,
    "questionIndex": 2,
    "totalQuestions": 15,
    "cefrLevel": "B1",
    "skill": "PRONUNCIATION",
    "questionType": "PRONUNCIATION",
    "timeoutSeconds": 30,
    "content": {
      "word": "comfortable",
      "ipaTranscription": "/ˈkʌmf.tə.bəl/",
      "instruction": "Đọc to từ bên dưới vào microphone"
    },
    "nextQuestion": {
      "questionId": 43,
      "questionIndex": 2,
      "totalQuestions": 15,
      "cefrLevel": "B1",
      "skill": "PRONUNCIATION",
      "questionType": "PRONUNCIATION",
      "timeoutSeconds": 30,
      "content": {
        "word": "comfortable",
        "ipaTranscription": "/ˈkʌmf.tə.bəl/",
        "instruction": "Đọc to từ bên dưới vào microphone"
      }
    }
  }
}
```
- **Response (Khi bài test kết thúc - `isTestCompleted: true`):**
```json
{
  "code": 1000,
  "message": "Thành công",
  "data": {
    "sessionId": 105,
    "submittedQuestionId": 56,
    "sessionStatus": "COMPLETED",
    "isTestCompleted": true,
    "previousAnswerCorrect": true,
    "previousCorrectAnswer": "option_a",
    "placementResult": {
      "cefrLevel": "B1",
      "skillScores": {
        "vocab": 75,
        "grammar": 80,
        "reading": 70,
        "listening": 65,
        "pronunciation": 72
      },
      "roadmapGenerated": true,
      "diagnosticTips": [
        "Bạn cần luyện thêm về phát âm trọng âm từ nhiều âm tiết."
      ]
    }
  }
}
```

---

### 5. Nộp câu trả lời Phát âm (Audio) trong Placement Test
- **Endpoint:** `POST /api/v1/onboarding/placement-test/pronunciation/submit-answer`
- **Content-Type:** `multipart/form-data`
- **Form Data:**
  - `sessionId`: `105` (Long)
  - `questionId`: `43` (Long)
  - `audioFile`: file binary `.wav`, `.webm`, hoặc `.ogg`
  - `word`: `"comfortable"` (String)
  - `wordIndex`: `0` (int, optional)
- **Response (Trả cả kết quả chấm audio VÀ câu tiếp theo / kết quả):**
```json
{
  "code": 1000,
  "message": "Thành công",
  "data": {
    "sessionId": 105,
    "submittedQuestionId": 43,
    "sessionStatus": "IN_PROGRESS",
    "isTestCompleted": false,
    "pronunciationResult": {
      "word": "comfortable",
      "overallScore": 82,
      "accuracyScore": 85,
      "fluencyScore": 80,
      "completenessScore": 100,
      "scoreColor": "GREEN",
      "scoreLevel": "EXCELLENT",
      "status": "SCORED"
    },
    "nextQuestion": {
      "sessionId": 105,
      "sessionStatus": "IN_PROGRESS",
      "questionId": 44,
      "questionIndex": 3,
      "totalQuestions": 15,
      "cefrLevel": "B1",
      "skill": "GRAMMAR",
      "questionType": "MULTIPLE_CHOICE",
      "timeoutSeconds": 45,
      "content": {
        "question": "If I ___ you, I would study harder.",
        "options": ["were", "was", "am", "be"]
      }
    },
    "placementResult": null
  }
}
```

---

### 6. Bỏ qua bài kiểm tra phân loại (Skip Test)
- **Endpoint:** `POST /api/v1/onboarding/placement-test/skip`
- **Tác dụng:** Gán level `A1`, điểm sàn 20/100, đánh dấu `isPlacementSkipped = true`, tự sinh lộ trình học ban đầu.
- **Response:**
```json
{
  "code": 1000,
  "message": "Thành công",
  "data": {
    "cefrLevel": "A1",
    "skillScores": {
      "vocab": 20,
      "grammar": 20,
      "reading": 20,
      "listening": 20,
      "pronunciation": 20
    },
    "roadmapGenerated": true
  }
}
```

---

### 7. Cài đặt mục tiêu ngày & giờ nhắc học (Settings)
- **Endpoint:** `POST /api/v1/onboarding/settings`
- **Payload:**
```json
{
  "dailyGoalXp": 50,
  "reminderTime": "20:30"
}
```
- **Response:**
```json
{
  "code": 1000,
  "message": "Cài đặt thành công",
  "data": {
    "settingsCompleted": true,
    "nextStep": "COMPLETE"
  }
}
```

---

### 8. Hoàn thành Onboarding (Complete)
- **Endpoint:** `POST /api/v1/onboarding/complete`
- **Response:**
```json
{
  "code": 1000,
  "message": "Hoàn thành onboarding thành công",
  "data": {
    "onboardingCompleted": true,
    "nextStep": "COMPLETED"
  }
}
```

---

## 🎙️ CHI TIẾT CONTRACT CÁC ENDPOINT MODULE 1: IPA & PHÁT ÂM

### 1. Chi tiết 1 âm IPA (kèm trạng thái Bookmark của user)
- **Endpoint:** `GET /api/v1/ipa/phonemes/{id}`
- **Headers:** `Authorization: Bearer <token>`
- **Response mẫu:**
```json
{
  "code": 1000,
  "message": "Thành công",
  "data": {
    "id": 1,
    "symbol": "iː",
    "phonemeType": "VOWEL_MONO",
    "nameVi": "Âm i dài",
    "audioMaleUrl": "https://storage.example.com/audio/male/i_long.mp3",
    "audioFemaleUrl": "https://storage.example.com/audio/female/i_long.mp3",
    "videoMouthUrl": "https://storage.example.com/video/mouth/i_long.mp4",
    "isCommonVnError": true,
    "pronunciationTipVi": "Kéo dài âm 'i' và mở rộng khóe miệng sang hai bên như đang cười.",
    "cefrIntroLevel": "A1",
    "isBookmarked": true,
    "exampleWords": [
      {
        "id": 10,
        "word": "sheep",
        "ipaTranscription": "/ʃiːp/",
        "audioUrl": "https://storage.example.com/words/sheep.mp3",
        "ttsUrl": "/api/v1/ipa/phonemes/tts/stream?text=sheep&type=WORD&voice=FEMALE"
      }
    ]
  }
}
```

---

### 2. Lưu / Bỏ lưu âm IPA (Toggle Bookmark - Typed Response)
- **Endpoint:** `POST /api/v1/ipa/phonemes/{id}/bookmark`
- **Response:**
```json
{
  "code": 1000,
  "message": "Thành công",
  "data": {
    "phonemeId": 1,
    "isBookmarked": true
  }
}
```

---

### 3. AI Chấm điểm phát âm luyện từ (Detailed Phoneme Feedback)
- **Endpoint:** `POST /api/v1/ipa/phonemes/practice`
- **Content-Type:** `multipart/form-data`
- **Form Data:**
  - `exampleWordId`: `10` (Long)
  - `audio`: file binary `.wav`, `.webm`, hoặc `.ogg`
- **Response:**
```json
{
  "code": 1000,
  "message": "Chấm điểm phát âm thành công",
  "data": {
    "practiceId": 1284,
    "exampleWordId": 10,
    "phonemeId": 1,
    "overallScore": 76,
    "scoreLevel": "GOOD",
    "fluencyScore": 80,
    "completenessScore": 100,
    "stressCorrect": true,
    "phonemes": [
      {
        "phoneme": "ʃ",
        "expectedPhoneme": "ʃ",
        "recognizedPhoneme": "ʃ",
        "accuracyScore": 92,
        "errorType": "NONE",
        "correctionHint": null,
        "scoreLevel": "EXCELLENT",
        "color": "GREEN"
      },
      {
        "phoneme": "iː",
        "expectedPhoneme": "iː",
        "recognizedPhoneme": "ɪ",
        "accuracyScore": 54,
        "errorType": "SUBSTITUTION",
        "correctionHint": "Kéo dài âm 'i' hơn và mở rộng khuôn miệng sang 2 bên như đang cười.",
        "scoreLevel": "NEEDS_PRACTICE",
        "color": "RED"
      },
      {
        "phoneme": "p",
        "expectedPhoneme": "p",
        "recognizedPhoneme": "p",
        "accuracyScore": 85,
        "errorType": "NONE",
        "correctionHint": null,
        "scoreLevel": "EXCELLENT",
        "color": "GREEN"
      }
    ]
  }
}
```

---

### 4. Phát âm thanh động qua Azure TTS Stream
- **Endpoint:** `GET /api/v1/ipa/phonemes/tts/stream?text=comfortable&type=WORD&voice=FEMALE`
- **Query params:**
  - `text`: từ hoặc ký hiệu IPA (Bắt buộc, tối đa 150 ký tự)
  - `type`: `WORD` hoặc `PHONEME` (Mặc định `WORD`)
  - `voice`: `MALE` hoặc `FEMALE` (Mặc định `MALE`)
- **Headers trả về:**
  - `Content-Type: audio/mpeg`
  - `Cache-Control: public, max-age=86400`
- **Body:** Binary MP3 audio stream (phát được ngay trên `AudioPlayer` Flutter).
