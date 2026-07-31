package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.*;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests cho auto-save session timeout 30 phút — Module 0 Gap Analysis.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OnboardingService — Session Timeout Tests")
class OnboardingSessionTimeoutTest {

    @Mock private UserRepository                    userRepository;
    @Mock private OnboardingRepository              onboardingRepository;
    @Mock private PlacementTestSessionRepository    sessionRepository;
    @Mock private PlacementTestAnswerRepository     answerRepository;
    @Mock private QuestionRepository                questionRepository;
    @Mock private DailyGoalRepository               dailyGoalRepository;
    @Mock private StudentStatRepository             studentStatRepository;
    @Mock private RoadmapGenerationService          roadmapGenerationService;
    @Mock private ObjectMapper                      objectMapper;

    @InjectMocks
    private OnboardingService onboardingService;

    private User                mockUser;
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
        given(sessionRepository.findById(100L)).willReturn(Optional.of(activeSession));
        given(answerRepository.countBySessionId(100L)).willReturn(0L);
        given(answerRepository.findBySessionIdOrderByAnsweredAtAsc(100L)).willReturn(Collections.emptyList());
        given(questionRepository.findRandomByCefrLevelExcluding(any(), any())).willReturn(Collections.emptyList());

        assertThatCode(() -> onboardingService.getNextQuestion(100L, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Session hết hạn (45 phút trước) → throw PLACEMENT_TEST_EXPIRED + đánh dấu completed")
    void sessionExpired_45MinAgo_throwsExpiredException() {
        activeSession.setLastActivityAt(LocalDateTime.now().minusMinutes(45));
        given(sessionRepository.findById(100L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> onboardingService.getNextQuestion(100L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_TEST_EXPIRED));

        // Session phải được đánh dấu is_completed = true
        verify(sessionRepository).save(argThat(PlacementTestSession::getIsCompleted));
    }

    @Test
    @DisplayName("lastActivityAt=null, startedAt=5 phút trước → fallback hợp lệ, không throw")
    void noLastActivity_recentStartedAt_sessionValid() {
        activeSession.setLastActivityAt(null);
        activeSession.setStartedAt(LocalDateTime.now().minusMinutes(5));
        given(sessionRepository.findById(100L)).willReturn(Optional.of(activeSession));
        given(answerRepository.countBySessionId(100L)).willReturn(0L);
        given(answerRepository.findBySessionIdOrderByAnsweredAtAsc(100L)).willReturn(Collections.emptyList());
        given(questionRepository.findRandomByCefrLevelExcluding(any(), any())).willReturn(Collections.emptyList());

        assertThatCode(() -> onboardingService.getNextQuestion(100L, 1L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("lastActivityAt=null, startedAt=60 phút trước → hết hạn qua fallback")
    void noLastActivity_oldStartedAt_sessionExpired() {
        activeSession.setLastActivityAt(null);
        activeSession.setStartedAt(LocalDateTime.now().minusMinutes(60));
        given(sessionRepository.findById(100L)).willReturn(Optional.of(activeSession));

        assertThatThrownBy(() -> onboardingService.getNextQuestion(100L, 1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_TEST_EXPIRED));
    }
}
