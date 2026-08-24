# MODULE 1: HỌC PHÁT ÂM (IPA PRONUNCIATION) - BẢN KIẾN TRÚC CHI TIẾT

Tài liệu này đóng vai trò là kim chỉ nam cho toàn bộ Module 1. Mục tiêu của module là giúp học viên làm chủ bảng phiên âm quốc tế IPA, sửa lỗi chi tiết đến từng âm tiết thông qua công nghệ AI (Azure Cognitive Services). Quá trình triển khai được chia nhỏ thành 2 Phase chính.

## 1. Mục tiêu (Objective)

Xây dựng hệ thống IPA Sound Library trực quan, kết hợp với AI Assessment Engine chấm điểm phát âm chính xác ở cấp độ âm tiết (phoneme-level).

**Yêu cầu kỹ thuật cốt lõi:**
- Thời gian phản hồi của API phát âm ≤ 3 giây (p95).
- Tối ưu hoá truy vấn dữ liệu tĩnh (danh sách IPA) với độ trễ ≤ 50ms.
- Decouple (giảm thiểu phụ thuộc) giữa core logic và hệ thống Gamification.

## 2. Quyết định Kiến trúc Hệ thống (Architecture Decision Records - ADR)

### ADR 1: Caching Strategy (Redis + Cache Stampede Prevention)
- **Quyết định:** Tích hợp Spring Cache với Redis. Cache lại kết quả của API `/api/v1/ipa/phonemes` và `/api/v1/ipa/phonemes/{id}`.
- **Cơ chế TTL:** 24h, chỉ evict khi Admin update dữ liệu.
- **⚠️ Cache Stampede Prevention:** Bắt buộc dùng `@Cacheable(sync = true)`. Khi N thread đồng thời miss cache, Spring sẽ chỉ cho 1 thread query DB, các thread còn lại chờ kết quả — tránh N query ập vào Database cùng lúc (Thunder Herd Problem).
- **Lý do:** Thư viện IPA là dữ liệu gần như bất biến. Mỗi request gọi xuống PostgreSQL là lãng phí tài nguyên hoàn toàn không cần thiết.

### ADR 2: Event-Driven Architecture cho Gamification (với Transaction Boundary rõ ràng)
- **Quyết định:** Dùng `Spring ApplicationEventPublisher`. Sau khi lưu Log thành công, publish `PronunciationCompletedEvent`. Một Listener `@Async` chạy bất đồng bộ để cộng điểm XP.
- **⚠️ Transaction Boundary Quan Trọng:** `@Async` tạo thread mới — thread đó **không kế thừa transaction** của Service gọi. Do đó Listener **bắt buộc** phải khai báo `@Transactional(propagation = REQUIRES_NEW)` bên trong chính nó. Nếu thiếu điều này, mọi write vào DB trong Listener sẽ không có transaction bảo vệ.
- **⚠️ AOP Proxy Pitfall với `@Retryable`:** `@Retryable` hoạt động qua AOP Proxy. Nếu gắn `@Retryable` trực tiếp lên method trong cùng Bean với `@Async`, Spring **bypass Proxy** → Retry không bao giờ chạy dù exception xảy ra. Giải pháp bắt buộc: Tách logic retry vào một Spring Bean riêng biệt (`RetryableGamificationService`) và inject vào Listener để gọi qua bean boundary.
- **Lợi ích:** SRP, API phản hồi nhanh, decouple hoàn toàn với hệ thống Gamification.

### ADR 3: Data Storage Optimization (JSONB + GIN Index)
- **Quyết định:** Lưu kết quả phân tích âm tiết dưới dạng `JSONB` trong column `phoneme_detail_json` của bảng `pronunciation_practice_logs`.
- **⚠️ GIN Index bắt buộc:** Khi cần query thống kê theo phoneme cụ thể (ví dụ: "Điểm trung bình âm /ʃ/ của học viên X"), PostgreSQL sẽ phải Full Table Scan nếu không có index. Phải tạo GIN Index trên column này ngay từ đầu trong Flyway migration.
  ```sql
  CREATE INDEX idx_practice_log_phoneme ON pronunciation_practice_logs USING GIN (phoneme_detail_json);
  ```
- **Lý do:** Azure API trả về JSON lớn, không cần tạo bảng phụ phức tạp — nhưng phải đánh index ngay từ đầu để tránh nợ kỹ thuật khi hệ thống scale.

## 3. Đánh giá Thực trạng Mã Nguồn (Gap Analysis)

**Sẵn sàng (Done):**
- Cấu trúc Entities (`IpaPhoneme`, `IpaExampleWord`, `PronunciationPracticeLog`, `StudentPhonemeBookmark`).
- Service gọi API Azure AI (`AzureAudioAnalysisService`).

**Cần xây dựng (To-Do):**
- **Phase 1:** Repositories, Redis Cache config (`sync=true`), `IpaService`, Data Seeding (Flyway V8 + GIN Index), `serialVersionUID` cho các DTO.
- **Phase 2:** `RetryableGamificationService` (Bean riêng), `PronunciationEventListener` (`@Async` + `REQUIRES_NEW`), `IpaController`, File Size Limit config.

## 4. Kế hoạch Kiểm thử (Verification Plan)

1. **Cache Stampede Test:** Gọi đồng thời 50 request khi cache trống, đảm bảo Hibernate chỉ log ra **đúng 1 câu SQL**, không phải 50 câu.
2. **Transaction Boundary Test:** Giả lập Listener throw exception, đảm bảo bản ghi `pronunciation_practice_logs` **không bị rollback** (INSERT log phải độc lập hoàn toàn với Listener).
3. **Retry Test:** Giả lập `CannotAcquireLockException` trong `RetryableGamificationService`, xác nhận Spring thực sự retry đúng 3 lần (quan sát log `@Retryable`).
4. **OOM/Security Test:** Thử upload file > 5MB, đảm bảo Spring từ chối ngay ở filter layer với lỗi `MaxUploadSizeExceededException`, không reach tới Controller.
5. **GIN Index Test:** Chạy `EXPLAIN ANALYZE` cho query JSONB thống kê, đảm bảo query plan dùng `Bitmap Index Scan` chứ không phải `Seq Scan`.

---

## 5. Lộ trình Triển khai (Phasing)

- [PHASE 1: IPA Sound Library & Caching Strategy](PHASE_1_IPA_LIBRARY.md)
- [PHASE 2: AI Assessment Engine & Event-Driven Gamification](PHASE_2_ASSESSMENT_ENGINE.md)
