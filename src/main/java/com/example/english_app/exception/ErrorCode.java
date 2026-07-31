package com.example.english_app.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Authentication
    INVALID_CREDENTIALS(1001, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1002, "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1003, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED(1004, "Token đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1005, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    INVALID_RESET_LINK(1006, "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH(1010, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    INCORRECT_OLD_PASSWORD(1011, "Mật khẩu cũ không đúng", HttpStatus.BAD_REQUEST),

    // User
    USER_NOT_FOUND(2001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(2002, "Email đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_EXISTS(2003, "Số điện thoại đã được sử dụng", HttpStatus.BAD_REQUEST),

    // Google OAuth
    GOOGLE_LOGIN_RESTRICTED(3001, "Tài khoản này chỉ được đăng nhập bằng Google", HttpStatus.BAD_REQUEST),
    GOOGLE_PASSWORD_RESET_NOT_ALLOWED(3002, "Tài khoản Google không thể đặt lại mật khẩu", HttpStatus.BAD_REQUEST),

    // Validation
    INVALID_REQUEST(4001, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),

    // Onboarding
    ONBOARDING_ALREADY_COMPLETED(5001, "Bạn đã hoàn thành quá trình Onboarding", HttpStatus.BAD_REQUEST),
    PLACEMENT_TEST_ALREADY_COMPLETED(5002, "Bạn đã hoàn thành bài kiểm tra phân loại", HttpStatus.BAD_REQUEST),
    PLACEMENT_TEST_NOT_FOUND(5003, "Không tìm thấy bài kiểm tra phân loại", HttpStatus.NOT_FOUND),
    PLACEMENT_TEST_EXPIRED(5004, "Bài kiểm tra đã hết hạn", HttpStatus.BAD_REQUEST),
    AUDIO_PROCESSING_FAILED(5010, "Không thể xử lý audio", HttpStatus.BAD_REQUEST),
    PRONUNCIATION_UNAVAILABLE(5011, "Dịch vụ chấm phát âm tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),
    ROADMAP_NOT_GENERATED(5012, "Lộ trình chưa được tạo. Vui lòng hoàn thành bài kiểm tra phân loại trước.", HttpStatus.NOT_FOUND),

    // System
    SYSTEM_ERROR(9999, "Lỗi hệ thống máy chủ", HttpStatus.INTERNAL_SERVER_ERROR),
    // Vocabulary
    TOPIC_NOT_FOUND(8001, "Chủ đề không tồn tại", HttpStatus.NOT_FOUND),
    TOPIC_ALREADY_EXISTS(8002, "Chủ đề đã tồn tại", HttpStatus.BAD_REQUEST),
    VOCABULARY_NOT_FOUND(8003, "Từ vựng không tồn tại", HttpStatus.NOT_FOUND),
    VOCABULARY_ALREADY_EXISTS(8004, "Từ vựng đã tồn tại", HttpStatus.BAD_REQUEST),

    // AI Quota
    QUOTA_EXCEEDED(6001, "Bạn đã hết lượt sử dụng AI hôm nay. Vui lòng nâng cấp gói Premium!", HttpStatus.FORBIDDEN),

    // Subscription Plan
    PLAN_NOT_FOUND(7001, "Gói cước không tồn tại", HttpStatus.NOT_FOUND),
    PLAN_ALREADY_EXISTS(7002, "Tên gói cước đã tồn tại", HttpStatus.BAD_REQUEST),

    // IPA Module
    PHONEME_NOT_FOUND(9001, "Không tìm thấy âm IPA với id đã cho", HttpStatus.NOT_FOUND),
    EXAMPLE_WORD_NOT_FOUND(9002, "Không tìm thấy từ ví dụ với id đã cho", HttpStatus.NOT_FOUND);


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public AppException toException() {
        return new AppException(this);
    }
}