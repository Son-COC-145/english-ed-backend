package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.DailyGoalRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class OnboardingRoadmapLifecycleTest {

    @Mock private UserRepository userRepository;
    @Mock private OnboardingRepository onboardingRepository;
    @Mock private PlacementTestSessionRepository sessionRepository;
    @Mock private DailyGoalRepository dailyGoalRepository;
    @Mock private StudentStatRepository studentStatRepository;
    @Mock private RoadmapJobService roadmapJobService;

    private OnboardingLifecycleService service;
    private StudentOnboarding onboarding;

    @BeforeEach
    void setUp() {
        service = new OnboardingLifecycleService(
                userRepository, onboardingRepository, sessionRepository,
                dailyGoalRepository, studentStatRepository, new ObjectMapper(), roadmapJobService);
        onboarding = StudentOnboarding.builder()
                .student(User.builder().id(1L).fullName("Learner").build())
                .goalSurveyJson("{}")
                .placementCefrLevel(CefrLevel.B1)
                .roadmapGenerationVersion(1)
                .roadmapGenerationAttempts(1)
                .build();
        lenient().when(sessionRepository.findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(1L))
                .thenReturn(Optional.empty());
    }

    @Test
    void pendingRoadmapExposesPollingStateAndRetryHint() {
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.PENDING);
        when(onboardingRepository.findByStudentIdWithUser(1L)).thenReturn(Optional.of(onboarding));

        OnboardingStatusResponse status = service.getStatus(1L);

        assertThat(status.getNextStep()).isEqualTo("ROADMAP_GENERATING");
        assertThat(status.getStepNumber()).isEqualTo(3);
        assertThat(status.getRoadmapStatus()).isEqualTo("PENDING");
        assertThat(status.getRoadmapRetryAfterMs()).isEqualTo(1500L);
        assertThat(status.isRoadmapGenerated()).isFalse();
    }

    @Test
    void failedRoadmapExposesExplicitRecoverableState() {
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.FAILED);
        when(onboardingRepository.findByStudentIdWithUser(1L)).thenReturn(Optional.of(onboarding));

        OnboardingStatusResponse status = service.getStatus(1L);

        assertThat(status.getNextStep()).isEqualTo("ROADMAP_FAILED");
        assertThat(status.getRoadmapRetryAfterMs()).isNull();
    }

    @Test
    void getRoadmapWhileWorkerIsRunningReturnsGeneratingError() {
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.PROCESSING);
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));

        assertThatThrownBy(() -> service.getRoadmap(1L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_GENERATING));
    }

    @Test
    void readyRoadmapAllowsFlowToContinueToSettings() {
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.READY);
        onboarding.setRoadmapJson("{\"cefrLevel\":\"B1\",\"milestones\":[]}");
        when(onboardingRepository.findByStudentIdWithUser(1L)).thenReturn(Optional.of(onboarding));

        OnboardingStatusResponse status = service.getStatus(1L);

        assertThat(status.getNextStep()).isEqualTo("SETTINGS");
        assertThat(status.isRoadmapGenerated()).isTrue();
        assertThat(status.getRoadmapStatus()).isEqualTo("READY");
    }
}
