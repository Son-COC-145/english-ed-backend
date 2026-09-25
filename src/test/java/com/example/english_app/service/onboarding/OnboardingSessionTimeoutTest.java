package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Unit tests cho auto-save session timeout 30 phút.
 * Đã migrate từ OnboardingService → PlacementTestService sau refactoring SRP.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PlacementTestService — Session Timeout Tests")
class OnboardingSessionTimeoutTest {

    @Mock private UserRepository                 userRepository;
    @Mock private OnboardingRepository           onboardingRepository;
    @Mock private PlacementTestSessionRepository sessionRepository;
    @Mock private PlacementTestAnswerRepository  answerRepository;
    @Mock private QuestionRepository             questionRepository;
    @Mock private RoadmapGenerationService       roadmapGenerationService;
    @Mock private PlacementResultFactory         resultFactory;
    @Mock private ObjectMapper                   objectMapper;
    @Mock private PlacementQuestionContentMapper questionContentMapper;
    @Mock private PlacementSessionExpiryService expiryService;
    @Mock private RoadmapJobService roadmapJobService;

    @InjectMocks
    private PlacementTestService placementTestService;

    private User                 mockUser;
    private PlacementTestSession activeSession;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).build();

        activeSession = PlacementTestSession.builder()
                .id(100L)
                .student(mockUser)
                .isCompleted(false)
                .currentCefrEstimate(CefrLevel.B1)
                .startedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Session còn hạn (10 phút trước) → không throw PLACEMENT_TEST_EXPIRED")
    void sessionActive_within30Min_continuesNormally() {
        activeSession.setLastActivityAt(LocalDateTime.now().minusMinutes(10));
        given(sessionRepository.findByIdWithStudentForUpdate(100L)).willReturn(Optional.of(activeSession));
        given(questionRepository.findBestAvailableForPlacement(any(), any(), any(Integer.class)))
                .willReturn(Optional.empty());

        // PLACEMENT_TEST_ALREADY_COMPLETED vì hết câu hỏi trong ngân hàng — nhưng KHÔNG phải EXPIRED
        assertThatThrownBy(() -> placementTestService.getNextQuestion(100L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isNotEqualTo(ErrorCode.PLACEMENT_TEST_EXPIRED));
    }

    @Test
    @DisplayName("Session hết hạn (45 phút trước) → throw PLACEMENT_TEST_EXPIRED + đánh dấu completed")
    void sessionExpired_45MinAgo_throwsExpiredException() {
        activeSession.setLastActivityAt(LocalDateTime.now().minusMinutes(45));
        given(expiryService.closeIfExpired(100L, 1L)).willReturn(true);
        given(sessionRepository.findByIdWithStudentForUpdate(100L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> placementTestService.getNextQuestion(100L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_TEST_EXPIRED));

        // Session phải được đánh dấu is_completed = true
        verify(expiryService).closeIfExpired(100L, 1L);
    }

    @Test
    @DisplayName("lastActivityAt=null, startedAt=5 phút trước → fallback hợp lệ, không EXPIRED")
    void noLastActivity_recentStartedAt_sessionValid() {
        activeSession.setLastActivityAt(null);
        activeSession.setStartedAt(LocalDateTime.now().minusMinutes(5));
        given(sessionRepository.findByIdWithStudentForUpdate(100L)).willReturn(Optional.of(activeSession));
        given(questionRepository.findBestAvailableForPlacement(any(), any(), any(Integer.class)))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> placementTestService.getNextQuestion(100L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isNotEqualTo(ErrorCode.PLACEMENT_TEST_EXPIRED));
    }

    @Test
    @DisplayName("lastActivityAt=null, startedAt=60 phút trước → hết hạn qua fallback")
    void noLastActivity_oldStartedAt_sessionExpired() {
        activeSession.setLastActivityAt(null);
        activeSession.setStartedAt(LocalDateTime.now().minusMinutes(60));
        given(expiryService.closeIfExpired(100L, 1L)).willReturn(true);
        given(sessionRepository.findByIdWithStudentForUpdate(100L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> placementTestService.getNextQuestion(100L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_TEST_EXPIRED));
    }
}
