# PHASE 2: PLACEMENT TEST ENGINE & CAT ALGORITHM

Phần này đặc tả thuật toán lõi của Module 0: Bài thi phân loại trình độ theo chuẩn CEFR.

## 1. Yêu cầu Cấu trúc Bài thi
Bài thi gồm 4 phần:
1.  **Từ vựng & Ngữ pháp:** 15 câu, 4 phút.
2.  **Đọc hiểu:** 8 câu, 4 phút.
3.  **Nghe hiểu:** 7 câu, 4 phút.
4.  *(Phần 4: Phát âm nhanh sẽ tách riêng ở Phase 3 do có call 3rd-party API).*

## 2. Thuật toán Computerized Adaptive Testing (CAT)
Bài test không cố định số câu và độ khó. Hệ thống sẽ tự động điều chỉnh dựa trên kết quả trả lời của học viên.

**Logic hoạt động (Rule-based Engine):**
- **Trạng thái khởi tạo:** Học viên luôn bắt đầu với 1 câu hỏi ở cấp độ **A2** (Trung bình).
- **Quy tắc điều chỉnh (Adaptive Rule):**
  - Trả lời ĐÚNG $\rightarrow$ Câu tiếp theo lấy ngẫu nhiên trong pool độ khó TĂNG 1 bậc (Ví dụ: A2 $\rightarrow$ B1).
  - Trả lời SAI $\rightarrow$ Câu tiếp theo lấy ngẫu nhiên trong pool độ khó GIẢM 1 bậc (Ví dụ: A2 $\rightarrow$ A1).
- **Điều kiện dừng (Stop Condition):**
  - **Dừng sớm:** Nếu thuật toán đạt `confidence` $\ge$ 85% (Ví dụ: Đúng liên tiếp 4 câu ở level B1).
  - **Dừng sớm (Fail-fast):** Sai 3 câu liên tiếp ở cùng một level hoặc rớt xuống dưới A1.
  - **Timeout:** Hết 15 phút tổng thời gian bài thi.

## 3. Cơ chế Auto-save Session (Chống mất dữ liệu)
- Khi gọi `POST /api/v1/onboarding/placement-test/start`, Backend tạo 1 record `PlacementTestSession` với `status = IN_PROGRESS`.
- Sau mỗi câu trả lời (`POST /submit-answer`), cập nhật trường `last_activity_at`.
- Nếu Frontend rớt mạng, khi học viên đăng nhập lại $\rightarrow$ Kiểm tra Session hiện tại. Nếu `last_activity_at` cách thời điểm hiện tại $\le$ 30 phút $\rightarrow$ Cho phép làm tiếp bằng API `GET /next-question`. Quá 30 phút $\rightarrow$ Session bị hủy.

## 4. Báo cáo Kết quả (Result Report - Bước 4)
- Sau khi Submit hoàn thành, Backend tính toán:
  - Cấp độ CEFR chung.
  - Điểm thành phần để vẽ **Radar Chart** (Từ vựng, Ngữ pháp, Đọc, Nghe).
  - List Top 3 điểm mạnh / Top 3 điểm yếu.
- Phản hồi JSON cho Frontend render UI Badge và Biểu đồ.
