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

    // System
    SYSTEM_ERROR(9999, "Lỗi hệ thống máy chủ", HttpStatus.INTERNAL_SERVER_ERROR),
    // Vocabulary
    TOPIC_NOT_FOUND(5001, "Chủ đề không tồn tại", HttpStatus.NOT_FOUND),
    TOPIC_ALREADY_EXISTS(5002, "Chủ đề đã tồn tại", HttpStatus.BAD_REQUEST),
    VOCABULARY_NOT_FOUND(5001, "Từ vựng không tồn tại", HttpStatus.NOT_FOUND),
    VOCABULARY_ALREADY_EXISTS(5002, "Từ vựng đã tồn tại", HttpStatus.BAD_REQUEST);

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