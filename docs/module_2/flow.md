# Kiến trúc Luồng Xử Lý (Flow Architecture) - Module 2

*Tài liệu mô tả ĐẦY ĐỦ các luồng nghiệp vụ chuẩn và Đặc tả API (API Specification) chi tiết nhất cho hệ thống Học Từ Vựng, Minigame và Quản lý Gói cước.*

---

## 1. Luồng Quản trị Từ Vựng & AI Content Generation (Admin)
Quy trình Admin cấu hình dữ liệu chủ đề và tự động hóa tạo Flashcard bằng AI.

```mermaid
sequenceDiagram
    actor Admin
    participant System as Backend System
    participant AI_LLM as Text AI (Gemini/ChatGPT)
    participant AI_TTS as Audio AI (Cloud TTS)
    participant AI_Img as Image AI (Pollinations)
    participant DB as Database

    %% Quản lý Topic
    Admin->>System: CRUD Chủ đề (Topic)
    System->>DB: Cập nhật Topics
    System-->>Admin: Thành công
    
    %% Sinh từ vựng AI
    Admin->>System: Yêu cầu tạo từ vựng (Từ, Chủ đề, Level)
    
    par Xử lý Text & Audio
        System->>AI_LLM: Sinh IPA, Định nghĩa, Ví dụ, Hội thoại
        AI_LLM-->>System: Trả về JSON Data
        System->>AI_TTS: Dịch Audio (US & UK) từ Text
        AI_TTS-->>System: Trả về Audio URLs
    and Xử lý Hình ảnh
        System->>AI_Img: Tạo prompt vẽ ảnh từ Từ Vựng
        AI_Img-->>System: Trả về Image URL
    end
    
    System->>DB: Lưu bản nháp (Draft Vocabulary)
    System-->>Admin: Hiển thị Preview
    Admin->>System: Sửa đổi (Nếu cần) & Publish
    System->>DB: Cập nhật trạng thái = ACTIVE
    System-->>Admin: Hoàn tất
```

### 📦 Đặc tả API tương ứng

#### 1.1 Quản trị Chủ đề (TopicController)
- **`GET /api/v1/topic`**: Lấy danh sách Topic.
  - **Query:** `page`, `size`, `sort`.
  - **Response:** `Page<TopicResponse>` (id, name, description, image_url, status).
- **`POST /api/v1/topic`**: Tạo mới Topic.
  - **Body (JSON/Form-data):** `name`, `description`, `file (Image)`.
- **`PUT /api/v1/topic/{id}`**: Cập nhật Topic.
- **`DELETE /api/v1/topic/{id}`**: Xóa/Ẩn Topic.

#### 1.2 Sinh nội dung AI (AdminVocabularyController)
- **`POST /api/v1/admin/vocabularies/generate`**: Yêu cầu AI sinh nội dung.
  - **Body (JSON):** `{"topicId": 12, "words": ["apple", "banana"], "level": "A1"}`
  - **Response:** `200 OK` (Trả về list vocabulary preview ở trạng thái DRAFT).

---

## 2. Luồng Học Tập Flashcard (Student Flow)
Quy trình học viên truy cập vào các chủ đề và luyện tập lật thẻ flashcard.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend (Topic & Vocab)
    participant DB as Database

    Student->>App: Mở danh mục Từ Vựng
    App->>BE: GET /api/v1/topic (filter active)
    BE->>DB: Fetch danh sách Topics
    BE-->>App: Trả về Topics
    
    Student->>App: Bấm vào 1 Topic
    App->>BE: GET /api/v1/vocabulary?topicId={id}
    BE->>DB: Lọc danh sách Vocabulary
    BE-->>App: Trả về chi tiết (Audio, Image, IPA)
    
    App->>Student: Hiển thị UI lật thẻ (Flashcard)
    Student->>App: Bấm lật thẻ & nghe Audio (App lấy Cache)
```

### 📦 Đặc tả API tương ứng
- **`GET /api/v1/vocabulary?topicId={id}`** (VocabularyController)
  - **Query Params:** `topicId` (Long), `page` (int), `size` (int).
  - **Response (200 OK):**
    ```json
    {
      "content": [
        {
          "id": 1,
          "word": "Apple",
          "ipa": "/ˈæp.əl/",
          "meaning": "Quả táo",
          "audioUrlUs": "https://cdn.../apple_us.mp3",
          "imageUrl": "https://cdn.../apple.png",
          "examples": ["I eat an apple."]
        }
      ]
    }
    ```

---

## 3. Luồng Chơi & Chấm điểm Minigame
Vòng đời của mini-game, từ lúc học viên bắt đầu cho đến khi tính điểm tích lũy.

```mermaid
stateDiagram-v2
    [*] --> StartGame: Học viên mở Minigame
    
    state PlayGame {
        StartGame --> DisplayQuestion: Frontend đếm ngược
        DisplayQuestion --> UserAnswer: Học viên chọn/ghép từ
        UserAnswer --> DisplayQuestion: Câu tiếp theo
    }
    
    PlayGame --> SubmitResult: Hoàn thành / Hết giờ
    SubmitResult --> ValidateScore: Gửi Payload (Số câu đúng, Thời gian) về Backend
    ValidateScore --> GainXP: Tính toán Base XP + Bonus Time
    GainXP --> UpdateProgress: Cập nhật DB (Vocabulary Progress)
    UpdateProgress --> [*]
```

### 📦 Đặc tả API tương ứng

#### 3.1 Ghi nhận kết quả Game (MiniGameController / StudentGamificationController)
- **`POST /api/v1/gamification/minigame-results`** (hoặc submit endpoint tương tự)
  - **Request Body:** 
    ```json
    {
      "gameType": "MATCHING_WORDS",
      "topicId": 5,
      "correctAnswers": 10,
      "totalQuestions": 12,
      "timeTakenSeconds": 45
    }
    ```
  - **Response (200 OK):** Trả về số điểm XP đạt được và level hiện tại.

#### 3.2 Lấy thống kê và tiến độ
- **`GET /api/v1/gamification/stat`**: Lấy thông tin thống kê XP, Streak của user.
- **`GET /api/v1/gamification/vocabulary-progress`**: Lấy % hoàn thành các Topic từ vựng.

---

## 4. Luồng Thanh Toán VNPAY (Premium Subscription Flow)
Kiến trúc 2 luồng phản hồi bảo mật cho cổng thanh toán bên thứ ba.

```mermaid
sequenceDiagram
    actor User
    participant App as Mobile App
    participant BE as Backend (PaymentController)
    participant VNP as VNPAY Gateway
    participant DB as Database

    User->>App: Chọn mua gói Premium
    App->>BE: POST /api/v1/payments/create (planId)
    BE->>DB: Tạo PaymentTransaction (PENDING)
    BE->>BE: Ký mã Hash (HMAC SHA512)
    BE-->>App: Trả về Payment URL
    App->>VNP: Mở WebView chuyển hướng đến VNPAY
    
    User->>VNP: Thực hiện thanh toán (ATM/Visa/QR)
    
    par IPN Webhook (Luồng cập nhật nền - Bắt buộc)
        VNP->>BE: GET /api/v1/payments/vnpay-ipn
        BE->>BE: Xác thực chữ ký Hash
        BE->>DB: Cập nhật Transaction (SUCCESS) & Nâng cấp gói User
        BE-->>VNP: Trả mã HTTP 200 (OK)
    and User Redirect (Luồng Giao diện)
        VNP-->>App: Redirect về App qua vnpay-return (Deep Link)
        App->>User: Đóng WebView, check State, báo "Thành công"
    end
```

### 📦 Đặc tả API tương ứng
- **`GET /api/v1/subscription-plans`** (SubscriptionPlanController)
  - Lấy danh sách gói cước (Tên, Giá tiền, Các tính năng).
- **`POST /api/v1/payments/create`** (PaymentController)
  - **Request Body:** `{"planId": 2, "returnUrl": "myapp://payment-return"}`
  - **Response:** `{"paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?..."}`
- **`GET /api/v1/payments/vnpay-ipn`** (PaymentController)
  - Do Server VNPAY gọi ẩn dưới background. Nhận các tham số `vnp_SecureHash`, `vnp_TxnRef`,... để verify chữ ký và update trạng thái đơn hàng.
