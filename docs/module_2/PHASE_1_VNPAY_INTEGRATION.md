# GIAI ĐOẠN 1: TÍCH HỢP THANH TOÁN VNPAY

## 1. Mục tiêu (Objective)
Xây dựng luồng tạo mã thanh toán VNPAY an toàn bằng thuật toán HMAC SHA512 và xử lý giao dịch ngầm (IPN - Webhook) để nâng cấp tài khoản lên gói PREMIUM tự động.

## 2. Chi tiết thực hiện (Implementation Steps)

### Bước 1: Cấu hình biến môi trường (application.yml)
Thêm cấu hình VNPAY Sandbox vào `application.yml`:
```yaml
vnpay:
  vnp_TmnCode: "YOUR_TMN_CODE" # Lấy từ VNPay Sandbox
  vnp_HashSecret: "YOUR_HASH_SECRET"
  vnp_Url: "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
  vnp_ReturnUrl: "http://localhost:8080/api/v1/payments/vnpay-return"
```

### Bước 2: Xây dựng File Config (VnpayConfig.java)
- Tạo class `VnpayConfig` để định nghĩa thuật toán mã hóa chữ ký (HMAC SHA512).
- Tạo hàm `hmacSHA512(String key, String data)`: VNPAY yêu cầu bắt buộc mã hóa toàn bộ dữ liệu đơn hàng bằng secret key để chống giả mạo.

### Bước 3: Xây dựng Service (PaymentService.java)
**1. Hàm tạo URL Thanh toán (`createPaymentUrl`):**
- Lấy thông tin `SubscriptionPlan`.
- Tạo 1 bản ghi `UserSubscription` với trạng thái `PENDING_PAYMENT`.
- Tạo 1 bản ghi `PaymentTransaction` với trạng thái `PENDING`.
- Dùng thuật toán nối chuỗi (Query String) các tham số: `vnp_Amount`, `vnp_Command`, `vnp_CreateDate`, `vnp_IpAddr`, `vnp_OrderInfo`, `vnp_ReturnUrl`, `vnp_TxnRef`.
- Ký HMAC SHA512 tạo ra `vnp_SecureHash` nối vào cuối URL.
- Trả URL về cho Frontend redirect người dùng.

**2. Hàm xử lý IPN Webhook (`processVnpayIpn`):**
- VNPAY sẽ gọi ngầm vào hàm này sau khi user quét mã QR thành công.
- Thuật toán: Lấy toàn bộ tham số VNPAY gửi về, tính toán lại chữ ký HMAC SHA512.
- Nếu chữ ký khớp (hợp lệ) và `vnp_ResponseCode == "00"` -> Cập nhật `PaymentTransaction` = `SUCCESS`.
- Cập nhật `UserSubscription` = `ACTIVE` (chính thức cấp quyền Premium).
- Trả về cho VNPAY JSON: `{"RspCode": "00", "Message": "Confirm Success"}`.

### Bước 4: Xây dựng Controller (PaymentController.java)
- `POST /api/v1/payments/create`: Nhận `planId`, trả về chuỗi URL thanh toán.
- `GET /api/v1/payments/vnpay-return`: Redirect người dùng về UI (Thành công/Thất bại).
- `GET /api/v1/payments/vnpay-ipn`: Hứng Webhook (Hàm này tuyệt đối không yêu cầu Token/Auth để VNPAY có thể gọi vào).
