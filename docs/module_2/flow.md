# Kiến trúc Luồng Xử Lý (Flow Architecture) - Module 2

*Tài liệu mô tả ĐẦY ĐỦ các luồng nghiệp vụ chuẩn cho hệ thống Học Từ Vựng, Minigame và Quản lý Gói cước.*

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

---

## 5. Luồng Kiểm soát Rate Limit (Redis API Gateway)
Giới hạn lượt sử dụng AI cho các tài khoản gói Basic.

```mermaid
sequenceDiagram
    actor User
    participant BE as Backend System
    participant Redis as Redis Cache
    participant LLM as Third-party AI

    User->>BE: Yêu cầu tính năng AI (Hỏi chatbot/Sinh câu)
    BE->>BE: Kiểm tra Gói cước (Basic hay Premium)
    
    alt Là tài khoản PREMIUM
        BE->>LLM: Pass - Cho phép gọi AI ngay lập tức
    else Là tài khoản BASIC
        BE->>Redis: INCR key "ai_usage:{userId}:{date}"
        Redis-->>BE: Số lượt hiện tại
        
        alt Lượt < Giới hạn cho phép
            BE->>LLM: Cho phép gọi AI
        else Quá giới hạn
            BE-->>User: HTTP 429 / 403 (QUOTA_EXCEEDED) - Yêu cầu nâng cấp
        end
    end
