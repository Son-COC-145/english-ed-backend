package com.example.english_app.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    CLASSROOM_RESOURCE_IN_USE(9009, "Không thể xóa khóa học đang có dữ liệu liên quan (học viên, bài tập hoặc tài liệu)", HttpStatus.CONFLICT),

    // Authentication
    INVALID_CREDENTIALS(1001, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED(1002, "Tài khoản đã bị khóa", HttpStatus.FORBIDDEN),
    INVALID_TOKEN(1003, "Token không hợp lệ hoặc đã hết hạn", HttpStatus.UNAUTHORIZED),
    TOKEN_REVOKED(1004, "Token đã bị thu hồi", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED(1005, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    INVALID_RESET_LINK(1006, "Link đặt lại mật khẩu không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(1007, "Không có quyền truy cập", HttpStatus.UNAUTHORIZED),
    PASSWORD_MISMATCH(1010, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),
    INCORRECT_OLD_PASSWORD(1011, "Mật khẩu cũ không đúng", HttpStatus.BAD_REQUEST),

    // User
    USER_NOT_FOUND(2001, "Người dùng không tồn tại", HttpStatus.NOT_FOUND),
    EMAIL_ALREADY_EXISTS(2002, "Email đã được sử dụng", HttpStatus.BAD_REQUEST),
    PHONE_ALREADY_EXISTS(2003, "Số điện thoại đã được sử dụng", HttpStatus.BAD_REQUEST),

    INVALID_ACCOUNT_DELETION_TOKEN(2004, "Liên kết xác minh xóa tài khoản không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),

    // Google OAuth
    GOOGLE_LOGIN_RESTRICTED(3001, "Tài khoản này chỉ được đăng nhập bằng Google", HttpStatus.BAD_REQUEST),
    GOOGLE_PASSWORD_RESET_NOT_ALLOWED(3002, "Tài khoản Google không thể đặt lại mật khẩu", HttpStatus.BAD_REQUEST),
    INVALID_OAUTH2_CODE(3003, "Mã xác thực OAuth2 không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),

    // Validation
    INVALID_REQUEST(4001, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_FILE(4002, "File tải lên phải là ảnh hợp lệ và không vượt quá 5 MB", HttpStatus.BAD_REQUEST),
    INVALID_AUDIO_FILE(4003, "File âm thanh phải là MP3, WAV, WEBM hoặc M4A hợp lệ và không vượt quá 5 MB", HttpStatus.BAD_REQUEST),

    // Onboarding
    ONBOARDING_ALREADY_COMPLETED(5001, "Bạn đã hoàn thành quá trình Onboarding", HttpStatus.BAD_REQUEST),
    PLACEMENT_TEST_ALREADY_COMPLETED(5002, "Bạn đã hoàn thành bài kiểm tra phân loại", HttpStatus.BAD_REQUEST),
    PLACEMENT_TEST_NOT_FOUND(5003, "Không tìm thấy bài kiểm tra phân loại", HttpStatus.NOT_FOUND),
    PLACEMENT_TEST_EXPIRED(5004, "Bài kiểm tra đã hết hạn", HttpStatus.BAD_REQUEST),
    ANSWER_ALREADY_SUBMITTED(5005, "Câu hỏi này đã được trả lời trong phiên làm bài hiện tại", HttpStatus.CONFLICT),
    AUDIO_PROCESSING_FAILED(5009, "Không thể xử lý audio", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_AUDIO_FORMAT(5010, "Định dạng audio không được hỗ trợ. Vui lòng sử dụng WAV, MP3, WebM, OGG hoặc MP4/M4A", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    AUDIO_PAYLOAD_TOO_LARGE(5011, "Kích thước file audio vượt quá giới hạn cho phép (tối đa 5MB)", HttpStatus.PAYLOAD_TOO_LARGE),
    AUDIO_EMPTY_OR_CORRUPT(5012, "File audio rỗng hoặc không thể xử lý", HttpStatus.BAD_REQUEST),
    PRONUNCIATION_UNAVAILABLE(5013, "Dịch vụ chấm phát âm tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),
    ROADMAP_NOT_GENERATED(5014, "Lộ trình chưa được tạo. Vui lòng hoàn thành bài kiểm tra phân loại trước.", HttpStatus.NOT_FOUND),
    PLACEMENT_QUESTION_EXHAUSTED(5015, "Hết câu hỏi khả dụng cho kỹ năng này", HttpStatus.SERVICE_UNAVAILABLE),
    PLACEMENT_QUESTION_MISMATCH(5016, "Câu hỏi không khớp với trạng thái hiện tại của bài kiểm tra", HttpStatus.CONFLICT),
    PLACEMENT_TEST_INCOMPLETE(5017, "Bạn cần hoàn thành đủ số câu hỏi trước khi kết thúc bài kiểm tra", HttpStatus.CONFLICT),

    ROADMAP_GENERATING(5018, "Lộ trình đang được tạo, vui lòng thử lại sau", HttpStatus.CONFLICT),
    ROADMAP_GENERATION_FAILED(5019, "Không thể tạo lộ trình. Vui lòng yêu cầu thử lại", HttpStatus.SERVICE_UNAVAILABLE),

    IDEMPOTENCY_KEY_REUSED(5020, "Submission ID đã được dùng cho dữ liệu khác", HttpStatus.CONFLICT),
    SUBMISSION_IN_PROGRESS(5021, "Đáp án này đang được xử lý, vui lòng retry cùng submissionId", HttpStatus.CONFLICT),
    GOAL_SURVEY_REQUIRED(5022, "Vui lòng hoàn thành khảo sát mục tiêu trước khi làm bài kiểm tra phân loại", HttpStatus.CONFLICT),
    GOAL_SURVEY_LOCKED(5023, "Không thể thay đổi khảo sát mục tiêu sau khi đã bắt đầu bài kiểm tra phân loại", HttpStatus.CONFLICT),

    // AI Quota
    QUOTA_EXCEEDED(6001, "Bạn đã hết lượt sử dụng AI hôm nay. Vui lòng nâng cấp gói Premium!", HttpStatus.FORBIDDEN),

    // Subscription Plan & Payment
    PLAN_NOT_FOUND(7001, "Gói cước không tồn tại", HttpStatus.NOT_FOUND),
    PLAN_ALREADY_EXISTS(7002, "Tên gói cước đã tồn tại", HttpStatus.BAD_REQUEST),
    PAYMENT_TRANSACTION_NOT_FOUND(7003, "Giao dịch thanh toán không tồn tại", HttpStatus.NOT_FOUND),

    // Vocabulary
    TOPIC_NOT_FOUND(8001, "Chủ đề không tồn tại", HttpStatus.NOT_FOUND),
    TOPIC_ALREADY_EXISTS(8002, "Chủ đề đã tồn tại", HttpStatus.BAD_REQUEST),
    VOCABULARY_NOT_FOUND(8003, "Từ vựng không tồn tại", HttpStatus.NOT_FOUND),
    VOCABULARY_ALREADY_EXISTS(8004, "Từ vựng đã tồn tại", HttpStatus.BAD_REQUEST),
    VOCABULARY_PROGRESS_NOT_FOUND(8005, "Không tìm thấy tiến trình học từ vựng này", HttpStatus.NOT_FOUND),
    VOCABULARY_NOT_PUBLISHED(8006, "Từ vựng chưa được phát hành", HttpStatus.FORBIDDEN),
    INVALID_REVIEW_RATING(8007, "Mức đánh giá không hợp lệ", HttpStatus.BAD_REQUEST),
    /** attemptId đã được xử lý – trả 200 với cached result thay vì 409 để Mobile retry an toàn */
    DUPLICATE_ATTEMPT(8008, "Attempt này đã được xử lý", HttpStatus.OK),
    DUPLICATE_ATTEMPT_CONFLICT(8009, "AttemptId đã được dùng cho payload khác", HttpStatus.CONFLICT),
    MINIGAME_ROUND_NOT_FOUND(8010, "Lượt chơi mini-game không tồn tại", HttpStatus.NOT_FOUND),
    MINIGAME_ROUND_CLOSED(8011, "Lượt chơi mini-game đã kết thúc", HttpStatus.CONFLICT),
    MINIGAME_ROUND_EMPTY(8012, "Lượt chơi chưa có câu trả lời nào", HttpStatus.BAD_REQUEST),

    // Speaking Coach
    SCENARIO_NOT_FOUND(8101, "Kịch bản giao tiếp không tồn tại", HttpStatus.NOT_FOUND),
    SCENARIO_ALREADY_EXISTS(8102, "Tên kịch bản giao tiếp đã tồn tại", HttpStatus.BAD_REQUEST),
    SESSION_NOT_FOUND(8103, "Không tìm thấy phiên giao tiếp", HttpStatus.NOT_FOUND),

    SPEAKING_CONFLICT(8104, "Phiên hoặc lượt nói chưa sẵn sàng", HttpStatus.CONFLICT),
    SPEAKING_UNAVAILABLE(8105, "Dịch vụ hội thoại tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),
    SPEAKING_AUDIO_PENDING(8106, "Âm thanh đang được tạo, vui lòng chờ", HttpStatus.CONFLICT),
    SPEAKING_AUDIO_FAILED(8107, "Tạo âm thanh thất bại, vui lòng thử lại lượt nói", HttpStatus.CONFLICT),
    SPEAKING_AUDIO_MISSING(8108, "Không tìm thấy dữ liệu âm thanh", HttpStatus.NOT_FOUND),
    SPEAKING_AUDIO_NO_SPEECH(8112, "Không phát hiện tiếng nói trong bản ghi", HttpStatus.BAD_REQUEST),
    SPEAKING_TTS_INVALID_OUTPUT(8113, "Dịch vụ tạo giọng nói không trả về âm thanh MP3 hợp lệ", HttpStatus.SERVICE_UNAVAILABLE),
    SPEAKING_TTS_UNAVAILABLE(8114, "Dịch vụ tạo giọng nói tạm thời không khả dụng", HttpStatus.SERVICE_UNAVAILABLE),
    SPEAKING_TTS_INTERRUPTED(8115, "Quá trình tạo giọng nói bị gián đoạn", HttpStatus.SERVICE_UNAVAILABLE),

    // Classroom
    COURSE_NOT_FOUND(9001, "Khóa học không tồn tại", HttpStatus.NOT_FOUND),
    STUDENT_ALREADY_IN_COURSE(9002, "Học viên đã tham gia khóa học này", HttpStatus.BAD_REQUEST),
    MATERIAL_NOT_FOUND(9003, "Tài liệu giảng dạy không tồn tại", HttpStatus.NOT_FOUND),
    SYLLABUS_NOT_FOUND(9004, "Lộ trình học không tồn tại", HttpStatus.NOT_FOUND),
    ASSIGNMENT_NOT_FOUND(9005, "Bài tập không tồn tại", HttpStatus.NOT_FOUND),
    SUBMISSION_NOT_FOUND(9006, "Bài nộp không tồn tại", HttpStatus.NOT_FOUND),
    STUDENT_NOT_IN_COURSE(9007, "Học viên không thuộc khóa học này", HttpStatus.FORBIDDEN),
    COURSE_ACCESS_DENIED(9008, "Bạn không có quyền truy cập vào khóa học này", HttpStatus.FORBIDDEN),
    COURSE_HAS_STUDENTS(9010, "Không thể hủy kích hoạt khóa học đang có học viên đăng ký", HttpStatus.BAD_REQUEST),
    MATERIAL_CONTENT_UNAVAILABLE(9012, "Không thể tải nội dung tài liệu từ kho lưu trữ. Vui lòng tải lại tài liệu", HttpStatus.BAD_GATEWAY),
    ASSIGNMENT_HAS_SUBMISSIONS(9011, "Bài tập đã có bài nộp nên không thể xóa hoặc đổi nội dung được giao", HttpStatus.CONFLICT),

    // IPA Module
    PHONEME_NOT_FOUND(9101, "Không tìm thấy âm IPA với id đã cho", HttpStatus.NOT_FOUND),
    EXAMPLE_WORD_NOT_FOUND(9102, "Không tìm thấy từ ví dụ với id đã cho", HttpStatus.NOT_FOUND),
    PRONUNCIATION_RULE_NOT_FOUND(9103, "Không tìm thấy quy tắc phát âm", HttpStatus.NOT_FOUND),
    MINIMAL_PAIR_NOT_FOUND(9104, "Không tìm thấy cặp âm đối lập", HttpStatus.NOT_FOUND),

    // System
    SYSTEM_ERROR(9999, "Lỗi hệ thống máy chủ", HttpStatus.INTERNAL_SERVER_ERROR);

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
