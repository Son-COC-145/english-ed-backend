# Kiến trúc Luồng Xử Lý (Flow Architecture) - Module 3

*Tài liệu mô tả ĐẦY ĐỦ các luồng nghiệp vụ chuẩn cho hệ thống AI Speaking Coach (Phản xạ giao tiếp).*

---

## 1. Luồng Quản trị Kịch Bản (Admin/Teacher Flow)
Giáo viên hoặc Admin cấu hình các Scenario để luyện tập.

```mermaid
sequenceDiagram
    actor Teacher
    participant BE as Backend
    participant DB as Database

    Teacher->>BE: Tạo Kịch bản (Context, Hint phrases, CEFR level)
    BE->>DB: Lưu SpeakingScenario
    BE-->>Teacher: Thành công
    
    Teacher->>BE: Gán AI Prompt (Thiết lập Persona cho AI)
    BE->>DB: Cập nhật System Prompt cho Kịch bản
    
    Teacher->>BE: Bật Trạng thái (Activate)
    BE->>DB: is_active = true
    BE-->>Teacher: Kịch bản khả dụng cho Học viên
```

---

## 2. Luồng Khởi tạo Phiên Giao Tiếp (Session Initialization)
Thiết lập ngữ cảnh và chuẩn bị bộ nhớ cache cực nhanh để phục vụ hội thoại không độ trễ.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend System
    participant Redis as Redis Cache
    participant DB as Database

    Student->>App: Chọn Kịch Bản
    App->>BE: POST /api/v1/speaking-session/start
    
    BE->>DB: Load AI Persona Prompt từ Scenario
    BE->>DB: Tạo bản ghi SpeakingSession (Trạng thái ONGOING)
    
    BE->>Redis: Lưu AI Prompt & Lịch sử rỗng (TTL: 1h)
    
    BE-->>App: Trả về Session ID & Câu chào mở màn (Text + Audio)
    App->>Student: Play Audio câu chào
```

---

## 3. Đường ống Streaming Thời gian thực (Real-time Pipeline)
Đây là cốt lõi của Module 3, xử lý luồng Voice-to-Voice thông qua STT -> LLM -> TTS trả về dạng Stream SSE.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend Pipeline
    participant STT as Audio-to-Text (Gemini)
    participant LLM as Text-to-Text (Gemini)
    participant TTS as Text-to-Audio (ElevenLabs/Google)

    Student->>App: Giữ Mic nói xong (Push to talk)
    App->>BE: Gửi File Audio (.m4a/.wav) lên API /audio-input
    
    %% STT Phase
    BE->>STT: Gửi Audio để dịch sang chữ
    STT-->>BE: Trả về Transcript (Text)
    BE->>Redis: Push Transcript của Student vào Lịch sử
    
    %% SSE Setup
    App->>BE: Ngay lập tức mở kết nối SSE tới /stream-response
    
    %% LLM Streaming Phase
    BE->>LLM: Gửi Prompt + Mảng Lịch sử từ Redis (Stream Mode)
    
    loop Nhận từng chunk Text từ LLM
        LLM-->>BE: Text Chunk
        BE-->>App: (SSE Event: text) Đẩy chunk -> App chạy hiệu ứng Typing
    end
    
    %% TTS Phase
    BE->>Redis: Push Câu trả lời đầy đủ của AI vào Lịch sử
    BE->>TTS: Gửi Full Text để tổng hợp âm thanh
    TTS-->>BE: Audio URL
    BE-->>App: (SSE Event: audio) Đẩy Audio URL (Kết thúc stream)
    
    App->>Student: Tự động Play Audio AI trả lời
```

---

## 4. Luồng Phân tích & Đánh giá Hậu kỳ (Post-Conversation Evaluation)
Xử lý nền (Background job) chạy sau khi user kết thúc cuộc gọi để chấm lỗi ngữ pháp, trôi chảy và XP.

```mermaid
sequenceDiagram
    actor Student
    participant App as Mobile App
    participant BE as Backend (Job Background)
    participant LLM as AI Evaluator
    participant DB as Database
    participant Game as Gamification Service

    Student->>App: Bấm "Kết Thúc Cuộc Gọi"
    App->>BE: POST /api/v1/speaking-session/{sessionId}/end
    
    %% Background Processing Starts
    BE->>BE: Đổ History từ Redis xuống DB (SpeakingTurn)
    BE->>BE: Dọn dẹp RAM (Xóa Key Redis)
    
    par Đánh giá tự động
        BE->>BE: Tính Fluency (Đếm Filler words um, uh & WPM)
        BE->>LLM: Gửi Transcript toàn cuộc gọi để Check Ngữ Pháp (JSON Mode)
        LLM-->>BE: Trả về List Lỗi, Cách Sửa & Điểm Nhiệm vụ
    end
    
    BE->>DB: Gộp kết quả lưu vào evaluation_json của Session
    
    %% Kích hoạt Thưởng
    BE->>Game: Trigger XP cho việc hoàn thành hội thoại
    Game->>DB: Cập nhật Gamification
    
    BE-->>App: Trả về Báo cáo (Report) hoàn chỉnh
    App->>Student: Hiển thị giao diện "Sửa lỗi" và "Điểm XP"
```
