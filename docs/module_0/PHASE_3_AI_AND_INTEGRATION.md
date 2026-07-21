# PHASE 3: AI ROADMAP & AUDIO INTEGRATION

Phase này giải quyết 2 bài toán khó nhất của Module 0 liên quan đến việc tích hợp các service AI và đảm bảo hiệu năng hệ thống (độ trễ $\le$ 3 giây).

## 1. Bài thi Phát âm nhanh (Quick Pronunciation) với Azure
Nằm ở phần 4 của Placement Test. Yêu cầu: đọc 5 từ trong 3 phút, AI chấm điểm realtime.

**Luồng Xử lý (Audio Pipeline):**
1.  **Thu âm (Frontend):** Client đếm ngược 3 giây, thu âm định dạng WebM/OGG.
2.  **Gửi Audio (Client $\rightarrow$ Server):** Gửi qua API `POST /api/v1/onboarding/placement-test/submit-pronunciation` dưới dạng `MultipartFile`.
3.  **Forward (Server $\rightarrow$ Azure):** 
    - `AudioAnalysisService` nhận file, nén/convert (nếu cần) thành chuẩn WAV 16kHz Mono.
    - Gọi API **Azure Cognitive Services - Pronunciation Assessment API**.
4.  **Parse Result (Server $\rightarrow$ Client):** 
    - Azure trả về file JSON khổng lồ chứa `accuracyScore`, `fluencyScore`, `completenessScore` và list các `phonemes` bị sai.
    - Tại Module 0, Backend **chỉ lấy điểm tổng hợp (Score)** và phân loại màu sắc (Xanh $\ge$ 80%, Vàng 60-79%, Đỏ < 60%). Bỏ qua mảng lỗi chi tiết để tối ưu băng thông.
5.  **Fallback Mechanism:** Nếu Azure sập hoặc phản hồi $> 5$ giây, Backend throw `TimeoutException`, catch và gán điểm phát âm = "Chưa đánh giá" để không làm gián đoạn bài thi.

## 2. Engine Sinh Lộ trình Cánh nhân hóa (Hybrid Roadmap Generation)
Nằm ở Bước 5. Yêu cầu: AI sinh roadmap hiển thị dạng Timeline trực quan $\le$ 3 giây.

**Kiến trúc Lai (Hybrid Architecture - ADR 2):**
- Tránh việc gọi API của OpenAI/Claude trực tiếp lúc User ấn hoàn thành (vì LLM tốn khoảng 5-15 giây để sinh output, gây vi phạm non-functional requirement $\le$ 3 giây).
- **Thực thi:**
  - **Offline (Batch Processing bằng LLM):** Trước đó, Admin/Hệ thống dùng LLM để crawl và tự động phân loại hàng ngàn bài học (Topics, Vocabularies, Speaking Scenarios) và gán tag (Ví dụ: `Tag = IT, Level = B1, Skill = Listening`). Lưu toàn bộ vào Database (hoặc ElasticSearch).
  - **Online (Realtime Rule-based Engine):** Khi User hoàn thành Placement Test, hệ thống chạy **Thuật toán Java thuần** để lấy dữ liệu:
    `SELECT * FROM topics WHERE cefr_level = {user_cefr} AND category IN ({user_goals}) LIMIT 10;`
  - Java ghép nối dữ liệu này thành cục JSON `roadmap_json` và trả về cực nhanh (vài chục milliseconds).
