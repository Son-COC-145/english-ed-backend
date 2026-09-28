# Hướng dẫn tích hợp Google OAuth2

## Phạm vi

Luồng đăng nhập Google giữ nguyên thiết kế ban đầu:

- Client mở OAuth URL của backend.
- Backend thực hiện web-server OAuth flow với Google.
- Backend không đưa access token hoặc refresh token vào redirect URL.
- Backend trả một one-time code ngắn hạn; client đổi code này lấy JWT của ứng dụng.
- Học viên vẫn có thể đăng ký tài khoản LOCAL bằng email/password qua `/api/v1/auth/register`.

## Luồng đăng nhập

1. Client mở trình duyệt tại:

   ```text
   GET {BACKEND_URL}/oauth2/authorization/google
   ```

2. Google callback về backend:

   ```text
   GET {BACKEND_URL}/login/oauth2/code/google
   ```

3. Backend tìm user theo email Google:

   - Nếu email đã tồn tại, backend sử dụng user hiện có.
   - Nếu email chưa tồn tại, backend tạo user `GOOGLE/STUDENT`.

4. Backend sinh UUID, lưu vào Redis trong 60 giây:

   ```text
   oauth2_code:{code} -> userId
   ```

5. Backend redirect về URI đã cấu hình:

   ```text
   {SPRING_SECURITY_OAUTH2_REDIRECT_URI}?code={oneTimeCode}
   ```

6. Client đổi code lấy token:

   ```http
   POST /api/v1/auth/oauth2/exchange
   Content-Type: application/json

   {"code":"{oneTimeCode}"}
   ```

Redis consume code bằng thao tác `GETDEL` atomic. Code hết hạn, đã dùng hoặc bị hai request sử dụng đồng thời sẽ trả `INVALID_OAUTH2_CODE`.

## Authentication response

```json
{
  "accessToken": "...",
  "refreshToken": "...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": 15,
  "email": "student@example.com",
  "fullName": "Student",
  "avatarUrl": "https://...",
  "role": "STUDENT",
  "provider": "GOOGLE",
  "onboardingCompleted": false
}
```

`onboardingCompleted` là trạng thái tóm tắt lấy từ bảng `users`:

- `false`: Flutter gọi `/api/v1/onboarding/status` để tiếp tục đúng bước.
- `true`: Flutter có thể đi thẳng vào luồng chính.
- `null`: role hiện tại không phải `STUDENT`.

Response không chứa `nextStep`; trạng thái chi tiết vẫn thuộc `/api/v1/onboarding/status`.

## Cấu hình Google Cloud

OAuth client dùng loại **Web application**. Authorized redirect URI là callback của backend, ví dụ:

```text
https://english-app-backend-fvhdetejdng4aceg.japaneast-01.azurewebsites.net/login/oauth2/code/google
```

Local development:

```text
http://localhost:8080/login/oauth2/code/google
```

URI Flutter/frontend nhận one-time code không phải Google redirect URI; đó là redirect thứ hai do backend thực hiện.

## Environment variables

```text
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
SPRING_SECURITY_OAUTH2_REDIRECT_URI=englishapp://oauth2/redirect
REDIS_HOST=...
REDIS_PORT=6379
REDIS_PASSWORD=...
REDIS_SSL=true
```

Nếu sử dụng trang web thay vì Flutter để nhận code, đặt `SPRING_SECURITY_OAUTH2_REDIRECT_URI` thành trang callback tương ứng của frontend.
