# Module 2 – Tài liệu tích hợp Mobile và Admin

Module 2 gồm chủ đề/từ vựng, ôn tập SRS, gamification và thanh toán Premium. Tất cả API yêu cầu:

```http
Authorization: Bearer <access-token>
```

Base URL minh họa: `https://api.example.com`.

## 1. Học từ vựng trên mobile

### 1.1 Lấy chủ đề

```http
GET /api/v1/topic?isActive=true&page=0&size=20
```

Student chỉ nhận topic active. Mỗi item gồm `id`, `nameEn`, `nameVi`, `iconUrl`, `isActive`, `vocabularyCount`, `masteredCount`.

### 1.2 Lấy danh sách từ

```http
GET /api/v1/vocabulary?topicId=1&cefrLevel=B1&page=0&size=20
```

Với student, backend luôn ép `status=PUBLISHED`. Response là `PageResponse<VocabularyResponse>`.

```json
{"success":true,"data":{"content":[{"id":101,"word":"itinerary","ipaTranscription":"/aɪˈtɪnəreri/","cefrLevel":"B1","definitionVi":"Lịch trình","imageUrl":"https://...","audioUsUrl":"https://...","audioUkUrl":"https://...","exampleSentences":[],"collocations":[],"dialogue":[],"status":"PUBLISHED","userProgress":null}],"currentPage":0,"pageSize":20,"totalElements":1,"totalPages":1}}
```

### 1.3 Lấy chi tiết từ

```http
GET /api/v1/vocabulary/101
```

Student không thể đọc vocabulary draft/inactive. `userProgress` có thể là `null` nếu chưa học.

## 2. Ôn tập SRS

### 2.1 Tổng quan Home

```http
GET /api/v1/gamification/vocabulary-summary
```

Trả `total`, `newCount`, `learningCount`, `reviewingCount`, `masteredCount`, `dueTodayCount`.

### 2.2 Lấy từ đến hạn

```http
GET /api/v1/gamification/vocabulary-reviews/due?page=0&size=20&topicId=1&cefrLevel=B1
```

Backend tự dùng `nextReviewAt <= server time`; mobile không tự tính ngày. `dueCount` cùng áp dụng filter topic/CEFR với danh sách.

### 2.3 Submit đánh giá

```http
POST /api/v1/gamification/vocabulary-reviews/submit
Content-Type: application/json

{"vocabularyId":101,"rating":"GOOD","durationSeconds":5,"attemptId":"c8b3a1a0-4f5a-4b9b-8d1e-2c3f4e5a6b7c"}
```

`rating`: `AGAIN`, `HARD`, `FAIR`, `GOOD`, `EASY`.

```json
{"success":true,"data":{"vocabularyId":101,"previousStatus":"LEARNING","newStatus":"REVIEWING","nextReviewAt":"2026-09-13T10:30:00","intervalDays":3,"xpEarned":5,"totalXp":320,"currentStreak":6}}
```

Retry mạng phải dùng lại `attemptId`; cùng `(user, attemptId)` không cộng XP lần hai.

### 2.4 Xem progress

```http
GET /api/v1/gamification/vocabulary-progress?status=REVIEWING&dueOnly=true&page=0&size=20
GET /api/v1/gamification/vocabulary-progress/{progressId}
```

## 3. Daily mission và minigame

```http
GET /api/v1/gamification/daily-mission
```

Trả `reviewWords` (từ đến hạn) và `newWords` (từ mới từ course/syllabus active). Mặc định tối đa 15 từ ôn và 5 từ mới.

```http
POST /api/v1/vocabularies/minigames/submit
Content-Type: application/json

{"vocabularyId":101,"gameType":"LISTEN_CHOOSE","isCorrect":true,"durationSeconds":3,"attemptId":"e1f2a3b4-5c6d-7e8f-9a0b-1c2d3e4f5a6b"}
```

`gameType`: `LISTEN_CHOOSE`, `WORD_SCRAMBLE`, `FILL_CONTEXT`, `MATCHING_FLASH`. Response trả `xpEarned`, `totalXp`, `currentStreak`, `newVocabularyStatus`, `resultId`.

Lịch sử và stats:

```http
GET /api/v1/gamification/minigame-results?page=0&size=20
GET /api/v1/gamification/minigame-results/{id}
GET /api/v1/gamification/stat
```

## 4. Admin/Teacher quản lý dữ liệu

Topic (chỉ ADMIN):

```http
POST   /api/v1/topic
PUT    /api/v1/topic/{id}
DELETE /api/v1/topic/{id}
PATCH  /api/v1/topic/activate/{id}
PATCH  /api/v1/topic/deactivate/{id}
```

Vocabulary (ADMIN/TEACHER):

```http
POST   /api/v1/vocabulary
PUT    /api/v1/vocabulary/{id}
DELETE /api/v1/vocabulary/{id}
```

Sinh vocabulary AI:

```http
POST /api/v1/admin/vocabularies/generate?word=itinerary&topicId=1&cefr=B1
```

Vocabulary mới nên được review và publish trước khi mobile student nhìn thấy.

## 5. Thanh toán Premium (Tích hợp VNPay)

Quy trình thanh toán gồm **2 kênh callback độc lập**:
- **IPN (Server-to-Server)**: VNPay gọi ngầm sang Backend để cập nhật Database và cấp quyền Premium. User/Mobile **không** gọi kênh này.
- **Return URL (Client Redirect)**: VNPay redirect trình duyệt/WebView của người dùng về Backend để hiển thị kết quả cho người dùng.

### 5.1 Lấy danh sách gói cước
Mobile gọi API để hiển thị danh sách các gói (Plan) cho người dùng chọn:

```http
GET /api/v1/subscription-plans?page=0&size=20
GET /api/v1/subscription-plans/{id}
```

Response trả về danh sách các gói với các trường chính: `id`, `name` (BASIC, PREMIUM), `price`, `durationDays`, `aiPromptLimit`.

### 5.2 Tạo đơn & Lấy URL thanh toán VNPay
Khi người dùng chọn gói và bấm "Thanh toán", Mobile gửi request:

```http
POST /api/v1/payments/create?planId=2
Authorization: Bearer <access-token>
```

- **Backend xử lý**:
  1. Tạo `UserSubscription` trạng thái `PENDING_PAYMENT`.
  2. Tạo `PaymentTransaction` trạng thái `PENDING` kèm mã giao dịch `vnp_TxnRef`.
  3. Ký bảo mật HMAC SHA512 và tạo mã timeout 15 phút trong Redis (`payment_timeout:{vnp_TxnRef}`).
- **Backend response**: Trả về chuỗi `paymentUrl` (URL chuyển tiếp tới cổng VNPay).

### 5.3 Mở giao diện thanh toán trên Mobile
Mobile mở một **In-App WebView** hoặc **Custom Tab** với `paymentUrl` nhận được ở bước 5.2 để người dùng quét mã VNPay-QR hoặc nhập thẻ ATM/quốc tế.

### 5.4 Kênh 1: IPN Webhook (Server-to-Server ngầm)
> **Lưu ý quan trọng**: Mobile **tuyệt đối KHÔNG gọi** API này. Đây là giao tiếp ngầm giữa VNPay và Backend.

Khi giao dịch phát sinh kết quả, hệ thống VNPay tự động gọi ngầm vào Backend:

```http
GET /api/v1/payments/vnpay-ipn?...vnp_Params...&vnp_SecureHash=...
```

- **Backend xử lý**:
  1. Kiểm tra chữ ký bảo mật `vnp_SecureHash` bằng `vnp_HashSecret`. Nếu không khớp, từ chối cập nhật (`RspCode: 97`).
  2. Nếu hợp lệ và `vnp_ResponseCode == "00"` (thành công):
     - Chuyển `PaymentTransaction` sang `SUCCESS`.
     - Kích hoạt `UserSubscription` sang `ACTIVE` và tính thời hạn: `endDate = now + plan.durationDays`.
     - Xóa key timeout trong Redis.
  3. Phản hồi cho VNPay: `{"RspCode": "00", "Message": "Confirm Success"}`.

### 5.5 Kênh 2: Return URL (Điều hướng người dùng & đóng WebView)
Sau khi người dùng thanh toán xong trên cổng VNPay, VNPay sẽ điều hướng (redirect) WebView về:

```http
GET /api/v1/payments/vnpay-return?vnp_ResponseCode=00&vnp_TxnRef=...
```

- **Backend xử lý**: 
  - Đọc `vnp_ResponseCode`:
    - Nếu `"00"`: Redirect tiếp về `{frontendUrl}/payment-success`.
    - Khác `"00"`: Redirect tiếp về `{frontendUrl}/payment-failed`.
- **Nhiệm vụ của Mobile**:
  - Dùng sự kiện lắng nghe URL trên WebView (ví dụ `onNavigationStateChange` trong React Native hoặc `onNavigationRequest` trong Flutter).
  - Khi thấy URL chứa `/api/v1/payments/vnpay-return`, `/payment-success`, hoặc `/payment-failed`:
    1. **Đóng WebView** ngay lập tức.
    2. Lấy tham số `vnp_ResponseCode` trên URL:
       - `vnp_ResponseCode == "00"` hoặc URL là `/payment-success`: Hiển thị thông báo thanh toán thành công, chúc mừng nâng cấp Premium.
       - Khác `"00"` (ví dụ `24` là người dùng hủy): Hiển thị thông báo thất bại/đã hủy.
    3. (Khuyến nghị) Gọi lại API User Profile / Subscription để cập nhật trạng thái mới nhất từ server.

### 5.6 Cơ chế tự động hủy giao dịch (Timeout 15 phút)
- Nếu người dùng tắt WebView hoặc không hoàn tất thanh toán trong 15 phút:
  - Redis key `payment_timeout:{vnp_TxnRef}` sẽ hết hạn (Expire).
  - Background listener (`PaymentExpirationListener`) tự động cập nhật `PaymentTransaction` thành `FAILED` và `UserSubscription` thành `CANCELLED`. Mobile không cần gọi API hủy thủ công.

## 6. Quy tắc tích hợp

1. `page` bắt đầu từ `0`.
2. Không tự tính SRS, XP, streak hoặc trạng thái publish.
3. Giữ nguyên idempotency key khi retry POST có side effect.
4. Không đổi score `null` thành `0`; `null` nghĩa là chưa có dữ liệu.
5. Student chỉ được xem topic active và vocabulary published.
6. Các warning Spring Data Redis khi khởi động không ảnh hưởng JPA repository; chỉ xử lý khi ứng dụng báo startup failure.
