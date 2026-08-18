package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.AdminQuestionRequest;
import com.example.english_app.dto.response.AdminQuestionResponse;
import com.example.english_app.dto.response.PageResponse;
import com.example.english_app.dto.response.QuestionBankStatsResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.QuestionType;
import com.example.english_app.entity.enums.Skill;
import com.example.english_app.entity.question.Question;
import com.example.english_app.repository.question.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Placement Questions Service Tests")
class AdminQuestionServiceTest {

    @Mock private QuestionRepository questionRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private AdminQuestionServiceImpl adminQuestionService;

    private Question buildQuestion(Long id, CefrLevel level, Skill skill) {
        return Question.builder()
                .id(id)
                .cefrLevel(level)
                .skill(skill)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .contentJson("{\"question\":\"Test\",\"options\":[\"A\",\"B\"]}")
                .correctAnswer("Answer")
                .timeoutSeconds(30)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("Admin lấy danh sách câu hỏi phân trang")
    void testGetQuestions() {
        Question q = buildQuestion(1L, CefrLevel.A1, Skill.VOCABULARY);
        Pageable pageable = PageRequest.of(0, 10);
        given(questionRepository.findByFilters(eq(CefrLevel.A1), eq(Skill.VOCABULARY), eq(true), eq(pageable)))
                .willReturn(new PageImpl<>(List.of(q), pageable, 1));

        PageResponse<AdminQuestionResponse> response = adminQuestionService.getQuestions(
                CefrLevel.A1, Skill.VOCABULARY, true, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Admin tạo câu hỏi mới")
    void testCreateQuestion() {
        Question saved = buildQuestion(10L, CefrLevel.B1, Skill.GRAMMAR);
        given(questionRepository.save(any(Question.class))).willReturn(saved);

        AdminQuestionRequest request = AdminQuestionRequest.builder()
                .cefrLevel(CefrLevel.B1)
                .skill(Skill.GRAMMAR)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .contentJson("{\"question\":\"Test\",\"options\":[\"A\",\"B\"]}")
                .correctAnswer("Answer")
                .timeoutSeconds(30)
                .build();

        AdminQuestionResponse response = adminQuestionService.createQuestion(request);
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSkill()).isEqualTo(Skill.GRAMMAR);
    }

    @Test
    @DisplayName("Admin toggle activate câu hỏi")
    void testToggleActivate() {
        Question q = buildQuestion(5L, CefrLevel.A2, Skill.READING);
        q.setIsActive(true);
        given(questionRepository.findById(5L)).willReturn(Optional.of(q));
        given(questionRepository.save(any(Question.class))).willReturn(q);

        AdminQuestionResponse response = adminQuestionService.toggleActive(5L, false);
        verify(questionRepository).save(any(Question.class));
        // isActive sẽ được set = false trước khi save
        assertThat(q.getIsActive()).isFalse();
    }

    @Test
    @DisplayName("Admin xem thống kê ngân hàng câu hỏi")
    void testGetStats() {
        given(questionRepository.countByIsActiveTrue()).willReturn(40L);
        given(questionRepository.countByIsActiveFalse()).willReturn(5L);
        given(questionRepository.countActiveGroupByLevel()).willReturn(
                List.of(new Object[]{CefrLevel.A1, 10L}, new Object[]{CefrLevel.B1, 20L})
        );
        given(questionRepository.countActiveGroupBySkill()).willReturn(
                List.of(new Object[]{Skill.GRAMMAR, 15L}, new Object[]{Skill.VOCABULARY, 25L})
        );
        given(questionRepository.countGroupByLevelAndSkillAndActive()).willReturn(
                List.of(
                    new Object[]{CefrLevel.A1, Skill.GRAMMAR, true, 5L},
                    new Object[]{CefrLevel.A1, Skill.VOCABULARY, true, 5L}
                )
        );

        QuestionBankStatsResponse stats = adminQuestionService.getStats();

        assertThat(stats.getTotalActive()).isEqualTo(40L);
        assertThat(stats.getTotalInactive()).isEqualTo(5L);
        assertThat(stats.getByLevel()).containsKey(CefrLevel.A1);
        assertThat(stats.getBySkill()).containsKey(Skill.GRAMMAR);
        assertThat(stats.getBreakdown()).hasSize(2);
    }

    @Test
    @DisplayName("Admin tạo câu hỏi với schema JSON không hợp lệ sẽ ném ngoại lệ")
    void testCreateQuestion_InvalidSchema() {
        AdminQuestionRequest request = AdminQuestionRequest.builder()
                .cefrLevel(CefrLevel.B1)
                .skill(Skill.GRAMMAR)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .contentJson("{\"question\":\"Missing options field\"}")
                .correctAnswer("Answer")
                .timeoutSeconds(30)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(com.example.english_app.exception.AppException.class, () ->
                adminQuestionService.createQuestion(request)
        );
    }

    @Test
    @DisplayName("Admin xóa câu hỏi")
    void testDeleteQuestion() {
        given(questionRepository.existsById(10L)).willReturn(true);
        adminQuestionService.deleteQuestion(10L);
        verify(questionRepository).deleteById(10L);
    }
}

