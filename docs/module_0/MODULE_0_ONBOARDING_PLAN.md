# MODULE 0: TỔNG QUAN ONBOARDING & PLACEMENT TEST

Tài liệu này đóng vai trò là kim chỉ nam cho toàn bộ Module 0. Vì quy mô của module mở rộng lên 6 bước phức tạp, để đảm bảo tính chuyên nghiệp và dễ quản lý (dễ phân công task cho nhiều Dev), quá trình triển khai được chia nhỏ thành 3 Phase chính.

## 1. Mục tiêu (Objective)

Cập nhật và chuẩn hóa luồng Onboarding bám sát 100% đặc tả hệ thống. Phân định rõ tiến độ mã nguồn hiện tại, các quyết định kiến trúc lõi (CAT, Hybrid Roadmap) và kế hoạch kiểm thử nhằm đảm bảo các yêu cầu phi chức năng (hiệu năng, độ trễ).

## 2. Phân tích 6 Bước Onboarding (Luồng Nghiệp Vụ)

*   **Bước 1: Welcome Screen:** Giao diện chào mừng, API kiểm tra trạng thái i18n, animation (yêu cầu hoàn thành $\le$ 2 giây).
*   **Bước 2: Goal Survey:** Khảo sát 5 câu hỏi mục tiêu học tập, lưu trữ linh hoạt dưới định dạng JSONB.
*   **Bước 3: Placement Test (Cốt lõi hệ thống):**
    *   Cấu trúc 4 phần đánh giá (Từ vựng/Ngữ pháp, Đọc, Nghe, Phát âm nhanh).
    *   **Thuật toán CAT (Adaptive Testing):** Cơ chế điều chỉnh độ khó động (bắt đầu A2, tăng/giảm bậc theo kết quả). Tự động chốt trình độ khi `confidence` $\ge$ 85% hoặc sai 3 lần liên tiếp.
    *   **Auto-save:** Lưu nháp trạng thái (session) trong 30 phút.
*   **Bước 4: Result Report:** Trả về dữ liệu vẽ Radar Chart (4 trục kỹ năng), xác định top 3 điểm mạnh/yếu và tổng hợp CEFR.
*   **Bước 5: Personalized Roadmap:** Hệ thống tự động sinh lộ trình học cá nhân hóa dựa trên trình độ và mục tiêu (Yêu cầu API phản hồi $\le$ 3 giây).
*   **Bước 6: Daily Goal & Notification:** Cài đặt mục tiêu XP hàng ngày (10/20/30/50) và thiết lập lịch nhắc nhở.

## 3. Đánh giá Thực trạng Mã Nguồn (Gap Analysis)

*   **Đã hoàn thành (Done):**
    *   Thiết kế CSDL toàn bộ Module 0 (`student_onboarding`, `placement_test_sessions`, `placement_test_answers`).
    *   Logic lõi thuật toán CAT trong `OnboardingService`.
    *   Tối ưu hóa truy vấn (Giải quyết N+1 Query bằng `@EntityGraph`).
    *   Quản lý cấu hình biến môi trường (`@Value`).

*   **Cần phát triển thêm (To-Do):**
    *   **Cơ chế Auto-save:** Xử lý logic timeout 30 phút dựa trên trường `last_activity_at`.
    *   **Tích hợp Azure AI:** Viết `AudioAnalysisService` gọi sang Azure Pronunciation API để chấm điểm 5 từ phát âm nhanh.
    *   **Rule-based Roadmap Engine:** Xây dựng logic định tuyến lộ trình học tập ở Bước 5.

## 4. Quyết định Kiến trúc & Hệ thống (Architecture Decisions - ADR)

Đây là các quyết định kỹ thuật đã được chốt để giải quyết các bài toán phức tạp của hệ thống:

*   **ADR 1: Chi tiết hóa thuật toán CAT.** Luồng xử lý toán học, logic cập nhật CEFR và tính điểm `confidence` sẽ được tài liệu hóa chi tiết (bằng flowchart hoặc giả mã) vào một file Phụ lục riêng (hoặc ADR docs) để tách bạch logic nghiệp vụ khỏi tài liệu API contract.
*   **ADR 2: Sinh Roadmap bằng Kiến trúc Lai (Hybrid Architecture).** Để đảm bảo thời gian phản hồi $\le$ 3 giây, tuyệt đối **KHÔNG** dùng LLM gọi trực tiếp (real-time).
    *   Sử dụng LLM (offline) để xây dựng Đồ thị tri thức (Knowledge Graph) và gắn tag cho các bài học.
    *   Sử dụng Rule-based Engine bằng Java (online) đóng vai trò "phễu lọc" để nhặt và sắp xếp các bài học phù hợp với user từ Database.
*   **ADR 3: Tái sử dụng Audio Service.** Phần chấm phát âm đầu vào (Module 0) và phần luyện âm IPA (Module 1) sẽ dùng chung một service `AudioAnalysisService`. Tuy nhiên, Controller của Module 0 sẽ lược bỏ các chi tiết báo lỗi sâu (chỉ lấy điểm tổng) trước khi trả về Frontend, trong khi Module 1 sẽ giữ nguyên mảng lỗi chi tiết.

## 5. Kế hoạch Kiểm thử (Verification Plan)

Nhằm đảm bảo các tiêu chuẩn khắt khe về kỹ thuật và trải nghiệm:

1.  **Unit Test (Logic lõi):** Viết test case mô phỏng các kịch bản của thuật toán CAT (trả lời đúng liên tiếp, sai 3 lần liên tiếp) để xác minh tính chính xác của điểm `confidence` và kết quả CEFR cuối cùng.
2.  **Performance Test (Đảm bảo độ trễ):**
    *   Tích hợp bộ đếm thời gian (StopWatch/Interceptor) để đo lường API `GET /roadmap`. Mốc thời gian xử lý (p95) bắt buộc $\le$ 3 giây.
    *   Sử dụng Postman/JMeter test thử tải luồng gọi sang Azure Pronunciation API để đảm bảo kết quả trả về trong $\le$ 3 giây.
3.  **Data Integrity Test:** Kiểm chứng luồng ngắt kết nối (session timeout): đảm bảo session tự đóng sau 30 phút không hoạt động và học viên lấy lại đúng `current_question_index` nếu quay lại trong khoảng thời gian cho phép.

---

## 6. Lộ trình Triển khai (Phasing)
Để phát triển hiệu quả, Module 0 được chia làm 3 Phase tài liệu chi tiết:
- [PHASE 1: Survey & Settings](PHASE_1_SURVEY_AND_SETTINGS.md)
- [PHASE 2: Placement Test Engine & CAT Algorithm](PHASE_2_PLACEMENT_TEST_ENGINE.md)
- [PHASE 3: AI Roadmap & Audio Integration](PHASE_3_AI_AND_INTEGRATION.md)
