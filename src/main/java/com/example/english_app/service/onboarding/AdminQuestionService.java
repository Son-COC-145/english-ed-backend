package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.AdminQuestionRequest;
import com.example.english_app.dto.response.AdminQuestionResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.QuestionBankStatsResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.Skill;
import org.springframework.data.domain.Pageable;

public interface AdminQuestionService {
    PageResponse<AdminQuestionResponse> getQuestions(CefrLevel level, Skill skill, Boolean isActive, Pageable pageable);
    AdminQuestionResponse getQuestionById(Long id);
    AdminQuestionResponse createQuestion(AdminQuestionRequest request);
    AdminQuestionResponse updateQuestion(Long id, AdminQuestionRequest request);
    void deleteQuestion(Long id);

    /** Bật/tắt trạng thái active của câu hỏi mà không cần gửi lại toàn bộ payload. */
    AdminQuestionResponse toggleActive(Long id, boolean active);

    /** Thống kê ngân hàng câu hỏi (breakdown theo level × skill). */
    QuestionBankStatsResponse getStats();

    /** Tự động sinh audio cho các câu hỏi bị thiếu (ví dụ seed data xài link giả) */
    java.util.Map<String, Object> generateMissingAudio();
}
