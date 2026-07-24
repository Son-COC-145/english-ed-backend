# MODULE 1: HỌC PHÁT ÂM (IPA PRONUNCIATION) - BẢN KIẾN TRÚC CHI TIẾT

Tài liệu này đóng vai trò là kim chỉ nam cho toàn bộ Module 1. Mục tiêu của module là giúp học viên làm chủ bảng phiên âm quốc tế IPA, sửa lỗi chi tiết đến từng âm tiết thông qua công nghệ AI (Azure Cognitive Services). Quá trình triển khai được chia nhỏ thành 2 Phase chính để tối ưu kiểm soát chất lượng và kiến trúc hệ thống.

## 1. Mục tiêu (Objective)

Xây dựng hệ thống IPA Sound Library trực quan, kết hợp với AI Assessment Engine chấm điểm phát âm chính xác ở cấp độ âm tiết (phoneme-level).
**Yêu cầu kỹ thuật cốt lõi:**
- Thời gian phản hồi của API phát âm $\le$ 3 giây (p95).
- Tối ưu hoá truy vấn dữ liệu tĩnh (danh sách IPA) với độ trễ $\le$ 50ms.
- Decouple (giảm thiểu phụ thuộc) giữa core logic và hệ thống Gamification.

## 2. Quyết định Kiến trúc Hệ thống (Architecture Decision Records - ADR)

Là một Senior System Architect, tôi đã quyết định áp dụng các pattern sau cho Module 1:

*   **ADR 1: Caching Strategy (Sử dụng Redis)**
    *   **Ngữ cảnh:** Thư viện IPA (44 âm vị và từ ví dụ) là dữ liệu gần như **bất biến (static data)**. Nếu gọi xuống Database PostgreSQL cho mỗi request sẽ gây lãng phí tài nguyên và tăng độ trễ.
    *   **Quyết định:** Tích hợp Spring Cache với Redis. Cache lại kết quả của API `/api/v1/ipa/phonemes` và `/api/v1/ipa/phonemes/{id}`. 
    *   **Cơ chế:** Thiết lập TTL (Time To Live) khoảng 24h hoặc cache vĩnh viễn (chỉ evict khi Admin update dữ liệu - tính năng Admin sẽ làm ở Module sau).

*   **ADR 2: Event-Driven Architecture cho Gamification**
    *   **Ngữ cảnh:** Khi User phát âm xong, hệ thống cần lưu log (`PronunciationPracticeLog`) và cộng điểm XP (`StudentStat`). Nếu code tuần tự, luồng API sẽ bị chậm và dính chặt vào logic của Gamification (tight-coupling).
    *   **Quyết định:** Sử dụng **Spring ApplicationEventPublisher**. Sau khi lưu Log thành công, `IpaPronunciationService` chỉ cần publish một event `PronunciationCompletedEvent(userId, xpReward)`. Một Listener chạy bất đồng bộ (`@Async`) sẽ bắt event này và cộng điểm XP.
    *   **Lợi ích:** Đảm bảo Single Responsibility Principle (SRP) và giúp API phản hồi về Client nhanh hơn.

*   **ADR 3: Data Storage Optimization (JSONB & DTO Mapping)**
    *   **Ngữ cảnh:** Azure API trả về một JSON cực lớn chứa thông tin phân tích âm tiết. Ta không cần tạo bảng phụ (ví dụ `phoneme_practice_details`) vì dữ liệu này chủ yếu để hiển thị lịch sử (read-only).
    *   **Quyết định:** Lưu kết quả dạng `JSONB` trong column `phoneme_detail_json` của bảng `pronunciation_practice_logs`. Áp dụng ObjectMapper/Hibernate Types để parse tự động giữa Entity và DTO.

## 3. Đánh giá Thực trạng Mã Nguồn (Gap Analysis)

*   **Sẵn sàng (Done):**
    *   Cấu trúc Entities (`IpaPhoneme`, `IpaExampleWord`, `PronunciationPracticeLog`, `StudentPhonemeBookmark`).
    *   Service gọi API Azure AI (`AzureAudioAnalysisService`).
*   **Cần xây dựng (To-Do):**
    *   **Phase 1:** Repositories, Redis Cache config, `IpaService`, Data Seeding (Flyway V8).
    *   **Phase 2:** Event Publisher, `IpaPronunciationService`, `IpaController`.

## 4. Kế hoạch Kiểm thử (Verification Plan)

1.  **Cache Testing:** Đảm bảo khi gọi API Get Phonemes lần 2, Hibernate không sinh ra câu lệnh SQL nào trong console (Dữ liệu lấy 100% từ Redis).
2.  **Asynchronous Event Testing:** Đảm bảo Event được trigger và điểm XP được cộng thành công ở một Thread khác mà không block thread HTTP chính.
3.  **Data Integrity:** Verify dữ liệu JSONB lưu xuống PostgreSQL phải đúng chuẩn JSON, không bị escape character thừa (kiểm tra bằng DataGrip hoặc DBeaver).

---

## 5. Lộ trình Triển khai (Phasing)

- [PHASE 1: IPA Sound Library & Caching Strategy](PHASE_1_IPA_LIBRARY.md)
- [PHASE 2: AI Assessment Engine & Event-Driven Gamification](PHASE_2_ASSESSMENT_ENGINE.md)
