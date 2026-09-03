# Kiến trúc Luồng Xử Lý (Flow Architecture) - Module 1

*Tài liệu mô tả ĐẦY ĐỦ các luồng nghiệp vụ chuẩn và Đặc tả API (API Specification) chi tiết nhất cho hệ thống Học Phát Âm IPA, Cặp âm Dễ nhầm lẫn (Minimal Pairs), Quy tắc Ngữ âm (Pronunciation Rules), Chấm điểm Phát âm AI (Azure Speech Assessment Engine), Bảng nhiệt độ thành thạo (Mastery Heatmap) & Bookmark.*

---

## 1. Luồng Quản trị Âm IPA, Từ Ví Dụ & Quy tắc Ngữ Âm (Admin Flow)
Quy trình Admin quản trị dữ liệu 44 âm IPA, cấu hình video khẩu hình miệng, từ ví dụ kèm phát âm, cặp âm tối thiểu (Minimal Pairs) và tự động sinh Audio mẫu chuẩn bằng Azure TTS.

```mermaid
sequenceDiagram
    actor Admin
    participant System as Backend (AdminIpaController)
    participant TTS as Azure TTS Service
    participant Storage as Azure Blob Storage
    participant DB as PostgreSQL Database

    %% Quản lý Âm & Sinh Audio Tự Động
    Admin->>System: POST /api/v1/admin/ipa/phonemes/{id}/generate-audio
    System->>TTS: Tổng hợp phát âm âm vị (US Voice)
    TTS-->>System: Audio raw bytes
    System->>Storage: Tải lên Azure Blob Storage container ipa-audio
    Storage-->>System: Trả về Public Audio CDN URL
    System->>DB: Cập nhật audio_male_url / audio_female_url
    System-->>Admin: Hoàn tất sinh audio mẫu

    %% Quản trị Minimal Pairs
    Admin->>System: POST /api/v1/admin/ipa/minimal-pairs
    System->>DB: Lưu cặp âm dễ gây nhầm lẫn (sheep / ship, see / she...)
    System-->>Admin: Thành công

    %% Quản trị Quy tắc ngữ âm (Nối âm, Trọng âm)
    Admin->>System: POST /api/v1/admin/ipa/rules
    System->>DB: Lưu quy tắc ngữ âm kèm ví dụ minh họa
    System-->>Admin: Thành công
```

### 📦 Đặc tả API tương ứng (AdminIpaController)

#### 1.1 Quản trị Âm IPA & Ví dụ
- **`GET /api/v1/admin/ipa/phonemes`**: Danh sách âm IPA dành cho Admin.
- **`PUT /api/v1/admin/ipa/phonemes/{id}`**: Cập nhật thông tin âm IPA (tên tiếng Việt, video khẩu hình, mẹo phát âm cho người Việt).
- **`POST /api/v1/admin/ipa/phonemes/{id}/example-words`**: Thêm từ ví dụ cho âm.
- **`POST /api/v1/admin/ipa/phonemes/{id}/generate-audio`**: Kích hoạt Azure TTS tự động sinh audio mẫu cho âm và lưu Blob Storage.
- **`POST /api/v1/admin/ipa/minimal-pairs`**: Thêm mới cặp từ dễ gây nhầm lẫn (Minimal Pairs).
- **`POST /api/v1/admin/ipa/rules`**: Thêm quy tắc ngữ âm (Trọng âm, Ghép âm, Nối âm).

---

## 2. Luồng Học & Tra cứu Thư viện 44 Âm IPA, Cặp Âm & Quy tắc (Student Flow)
Quy trình học viên duyệt bảng phiên âm quốc tế IPA, xem chi tiết khẩu hình, nghe audio mẫu chuẩn (với cơ chế Fallback TTS Stream tức thì nếu từ thiếu audio) và lưu Bookmark để ôn tập.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App (Flutter)
    participant BE as Backend (IpaController)
    participant DB as Database
    participant TTS as Azure TTS Service

    %% Khám phá bảng IPA
    Student->>App: Mở Bảng Âm IPA
    App->>BE: GET /api/v1/ipa/phonemes?type=VOWEL
    BE->>DB: Lấy danh sách âm (Cacheable ipa_phonemes_v2)
    BE-->>App: Danh sách âm (Ký hiệu, Tên tiếng Việt, Audio URL)

    %% Xem chi tiết âm
    Student->>App: Chọn âm /iː/
    App->>BE: GET /api/v1/ipa/phonemes/{id}
    BE->>DB: Lấy chi tiết âm, danh sách từ ví dụ & check bookmark của user
    BE-->>App: IpaPhonemeDetailResponse (Video URL, Mẹo khẩu hình tiếng Việt, isBookmarked)

    %% Nghe Audio tức thì (Fallback Stream)
    alt Audio URL có sẵn trong DB
        Student->>App: Bấm nghe từ ví dụ "Sheep"
        App->>App: Phát audio từ CDN
    else Audio URL rỗng / không khả dụng
        App->>BE: GET /api/v1/ipa/phonemes/tts/stream?text=sheep&type=WORD&voice=MALE
        BE->>TTS: Tổng hợp âm thanh tức thì (SSML)
        BE-->>App: Trả về byte[] audio/mpeg trực tiếp (Cache 24h)
        App->>Student: Phát âm thanh trơn tru không bị delay
    end

    %% Bookmark âm
    Student->>App: Nhấn biểu tượng Bookmark ⭐
    App->>BE: POST /api/v1/ipa/phonemes/{id}/bookmark
    BE->>DB: Toggle lưu/bỏ lưu trong student_phoneme_bookmarks
    BE-->>App: IpaBookmarkResponse { "phonemeId": 1, "isBookmarked": true }
```

### 📦 Đặc tả API tương ứng (IpaController)

#### 2.1 Danh sách Âm IPA
- **`GET /api/v1/ipa/phonemes`**:
  - **Query Params:** `type` (VOWEL, DIPHTHONG, CONSONANT), `isCommonError` (boolean).
  - **Response (200 OK):** Danh sách âm kèm symbol, nameVi, audioMaleUrl, audioFemaleUrl.

#### 2.2 Chi tiết 1 Âm IPA (Kèm trạng thái Bookmark cá nhân hóa)
- **`GET /api/v1/ipa/phonemes/{id}`**:
  - **Response (200 OK):**
    ```json
    {
      "code": 1000,
      "data": {
        "id": 1,
        "symbol": "iː",
        "phonemeType": "VOWEL",
        "nameVi": "Âm i dài",
        "audioMaleUrl": "https://cdn.../i_long_male.mp3",
        "audioFemaleUrl": "https://cdn.../i_long_female.mp3",
        "videoMouthUrl": "https://cdn.../i_long_mouth.mp4",
        "isCommonVnError": true,
        "pronunciationTipVi": "Môi mở rộng sang hai bên như đang mỉm cười. Lưỡi nâng cao về phía trước, giữ âm ngân dài 1-2 giây.",
        "cefrIntroLevel": "A1",
        "isBookmarked": true,
        "exampleWords": [
          {
            "id": 101,
            "word": "sheep",
            "ipaTranscription": "/ʃiːp/",
            "meaningVi": "con cừu",
            "audioUrl": "https://cdn.../sheep.mp3"
          }
        ]
      }
    }
    ```

#### 2.3 Phát âm thanh động tức thì (Fallback TTS Stream)
- **`GET /api/v1/ipa/phonemes/tts/stream`**:
  - **Query Params:** `text` (String, max 150 chars), `type` (`WORD` | `PHONEME`), `voice` (`MALE` | `FEMALE`).
  - **Response:** `200 OK` (MIME Type: `audio/mpeg`, Cache-Control: `public, max-age=86400`).
  - **Mã lỗi:** `400 Bad Request` nếu thiếu text hoặc type/voice không hợp lệ.

#### 2.4 Quản lý Bookmark Âm IPA
- **`POST /api/v1/ipa/phonemes/{id}/bookmark`**: Toggle Bookmark âm.
  - **Response (200 OK):**
    ```json
    {
      "code": 1000,
      "data": {
        "phonemeId": 1,
        "isBookmarked": true
      }
    }
    ```
- **`GET /api/v1/ipa/phonemes/bookmarks`**: Lấy danh sách toàn bộ các âm đã bookmark của user (sắp xếp mới nhất trước).

#### 2.5 Danh sách Cặp âm dễ nhầm lẫn (Minimal Pairs)
- **`GET /api/v1/ipa/phonemes/minimal-pairs`**:
  - Trả về danh sách đối lập (sheep vs ship, see vs she...) kèm cả 2 audio để học viên so sánh phân biệt.

#### 2.6 Quy tắc Ngữ âm (Pronunciation Rules)
- **`GET /api/v1/ipa/phonemes/rules`**: Lọc theo `category` (`LINKING_SOUNDS`, `WORD_STRESS`, `SENTENCE_STRESS`, `INTONATION`).
- **`GET /api/v1/ipa/phonemes/rules/{id}`**: Chi tiết quy tắc kèm câu ví dụ minh họa và audio mẫu.

---

## 3. Luồng Luyện Tập & Chấm Điểm AI (AI Assessment Engine)
Quy trình học viên ghi âm phát âm một từ ví dụ của âm IPA, backend kiểm tra tính hợp lệ của file âm thanh (Magic bytes & dung lượng), gửi tới Azure Cognitive Services Pronunciation Assessment và phân tích chi tiết đến từng âm vị (Phoneme-level Feedback).

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend (IpaPronunciationService)
    participant Azure as Azure Speech Assessment
    participant Event as Spring Event Bus
    participant DB as Database

    Student->>App: Bấm giữ Micro & đọc từ "sheep"
    App->>BE: POST /api/v1/ipa/phonemes/practice (Multipart audio, exampleWordId)
    
    BE->>BE: Validate Magic bytes (WAV/WEBM/OGG) & Size (<= 5MB)
    alt Định dạng hoặc kích thước không hợp lệ
        BE-->>App: 415 / 413 / 400 kèm Machine-readable ErrorCode
    end

    BE->>Azure: Pronunciation-Assessment Header (Base64 JSON) + Audio Bytes
    Azure-->>BE: Kết quả JSON chi tiết (Overall, Accuracy, Fluency, Phonemes breakdown)

    BE->>DB: Lưu nhật ký vào pronunciation_practice_logs
    BE->>Event: Publish PronunciationCompletedEvent (Async)
    Note over Event: Tự động cộng XP và cập nhật DailyGoal ngầm không làm chậm response

    BE-->>App: Trả về PronunciationResultResponse (kèm errorType, correctionHint, scoreLevel)
    App->>Student: Hiển thị giao diện Highlight trực quan (Âm nào đúng màu Xanh, âm sai màu Đỏ)
```

### 🛡️ Chuẩn Mã Lỗi Audio Máy Đọc Được (Machine-Readable Error Codes)

| HTTP Status | Custom Code | Error Enum | Nguyên nhân | Mobile Xử lý |
|---|---|---|---|---|
| **415 Unsupported Media Type** | `5010` | `UNSUPPORTED_AUDIO_FORMAT` | Magic bytes không phải WAV/WEBM/OGG | Báo học viên kiểm tra định dạng ghi âm |
| **413 Payload Too Large** | `5011` | `AUDIO_PAYLOAD_TOO_LARGE` | File ghi âm vượt quá 5MB | Cắt ngắn thời gian ghi âm |
| **400 Bad Request** | `5012` | `AUDIO_EMPTY_OR_CORRUPT` | File audio rỗng (< 4 bytes) | Báo học viên ghi âm lại |
| **503 Service Unavailable** | `5013` | `PRONUNCIATION_UNAVAILABLE` | Azure timeout / bảo trì | Hiển thị thông báo thử lại sau |

### 📦 Đặc tả API tương ứng (IpaController)

#### 3.1 Chấm điểm phát âm từ ví dụ (AI Practice)
- **`POST /api/v1/ipa/phonemes/practice`**:
  - **Content-Type:** `multipart/form-data`
  - **Params:** `exampleWordId` (Long), `audio` (File audio).
  - **Response (200 OK):**
    ```json
    {
      "code": 1000,
      "message": "Thành công",
      "data": {
        "practiceId": 1205,
        "exampleWordId": 101,
        "phonemeId": 1,
        "overallScore": 84,
        "scoreLevel": "EXCELLENT",
        "fluencyScore": 82,
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
            "recognizedPhoneme": "iː",
            "accuracyScore": 85,
            "errorType": "NONE",
            "correctionHint": null,
            "scoreLevel": "EXCELLENT",
            "color": "GREEN"
          },
          {
            "phoneme": "p",
            "expectedPhoneme": "p",
            "recognizedPhoneme": "b",
            "accuracyScore": 55,
            "errorType": "SUBSTITUTION",
            "correctionHint": "Chú ý bật hơi nhẹ ở môi cho âm đuôi /p/, không phát âm thành /b/",
            "scoreLevel": "NEEDS_PRACTICE",
            "color": "RED"
          }
        ]
      }
    }
    ```

---

## 4. Luồng Bảng Nhiệt Độ Thành Thạo (Mastery Heatmap) & Lịch Sử
Hệ thống tổng hợp toàn bộ các lần luyện tập của học viên để xây dựng Bảng nhiệt độ thành thạo 44 âm IPA và cung cấp lịch sử luyện tập gần nhất.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend (IpaService)
    participant DB as Database

    Student->>App: Mở tab "Tiến độ IPA"
    App->>BE: GET /api/v1/ipa/phonemes/mastery
    BE->>DB: Aggregate điểm trung bình từng âm từ pronunciation_practice_logs
    BE-->>App: IpaMasterySummaryResponse (Số lượng Mastered/Learning/NeedsPractice/Unlearned & %)
    App->>Student: Render Ma trận 44 ô màu (Xanh lá, Vàng, Đỏ, Xám)

    Student->>App: Bấm vào 1 âm để xem lịch sử
    App->>BE: GET /api/v1/ipa/phonemes/{id}/history
    BE->>DB: Lấy 10 lần luyện tập gần nhất
    BE-->>App: Danh sách lịch sử (Từ đã đọc, điểm số, ngày luyện)
```

### 📦 Đặc tả API tương ứng
- **`GET /api/v1/ipa/phonemes/mastery`**: Bảng nhiệt độ thành thạo:
  - **Trạng thái:**
    - `MASTERED` (>= 80, Xanh lá)
    - `LEARNING` (60 - 79, Vàng)
    - `NEEDS_PRACTICE` (< 60, Đỏ)
    - `UNLEARNED` (Chưa từng luyện tập, Xám)
  - **Thống kê:** `totalPhonemes`, `masteredCount`, `learningCount`, `needsPracticeCount`, `unlearnedCount`, `overallMasteryPercent`.
- **`GET /api/v1/ipa/phonemes/{id}/history`**: Lịch sử 10 lần đọc gần nhất của học viên đối với âm tương ứng.
