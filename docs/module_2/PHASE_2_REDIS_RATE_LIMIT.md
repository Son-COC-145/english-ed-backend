# GIAI ĐOẠN 2: THIẾT LẬP REDIS RATE LIMITER (GIỚI HẠN AI)

## 1. Mục tiêu (Objective)
Đảm bảo học viên dùng gói Basic không thể lạm dụng tính năng AI (ví dụ: bị chặn sau khi gọi 5 lần/ngày). Sử dụng Redis để tối ưu tốc độ và không làm chậm Database chính.

## 2. Chi tiết thực hiện (Implementation Steps)

### Bước 1: Cấu hình Redis
- Đảm bảo trong `pom.xml` đã có `spring-boot-starter-data-redis`.
- Khai báo host/port Redis trong `application.yml`:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

### Bước 2: Xây dựng Service đếm số lần (AiRateLimiterService.java)
- Sử dụng `StringRedisTemplate`.
- **Hàm `checkAndIncrementUsage(Long userId, int limit)`:**
  - Định nghĩa key: `String key = "ai_usage:user:" + userId + ":date:" + LocalDate.now();`
  - Chạy lệnh `INCR` của Redis: `Long currentUsage = redisTemplate.opsForValue().increment(key);`
  - Khi `currentUsage == 1` (tức là lần gọi đầu tiên trong ngày), bắt buộc phải set TTL: `redisTemplate.expire(key, 24, TimeUnit.HOURS);` (để qua ngày mới nó tự biến mất).
  - So sánh: Nếu `currentUsage > limit` -> Return `false` (Báo lỗi vượt quá giới hạn).
  - Nếu `currentUsage <= limit` -> Return `true` (Được phép đi tiếp).

### Bước 3: Tích hợp vào Luồng gọi AI của User
- Khi Student gửi request (Ví dụ API `POST /api/v1/student/ai/chat`).
- Controller sẽ check gói cước của User trong CSDL:
  - Nếu gói = `PREMIUM` -> Bỏ qua Redis, cho phép chạy luôn logic AI.
  - Nếu gói = `BASIC` -> Lấy `limit` (ví dụ 5) từ CSDL. Gọi `AiRateLimiterService.checkAndIncrementUsage(userId, limit)`.
  - Nhận về `false` -> Quăng ra Exception (ErrorCode `QUOTA_EXCEEDED` -> Trả về HTTP 403 Forbidden).
  - Nhận về `true` -> Tiếp tục gọi AI thật.

### Lưu ý kiến trúc (Best Practice)
Nếu dự án có rất nhiều API cần chặn Rate Limit, thay vì check thủ công trong từng Controller, ta có thể viết logic này trong **Spring AOP (Aspect Oriented Programming)** bằng cách tạo một Annotation tùy chỉnh `@RateLimitedAi` gắn lên đầu Controller.
