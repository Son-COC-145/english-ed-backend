package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningGoal;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.user.User;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.gamification.DailyGoalRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.adaptive.roadmap.RoadmapProgressService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;

@ExtendWith(MockitoExtension.class)
class OnboardingRoadmapLifecycleTest {

    @Mock private UserRepository userRepository;
    @Mock private OnboardingRepository onboardingRepository;
    @Mock private PlacementTestSessionRepository sessionRepository;
    @Mock private DailyGoalRepository dailyGoalRepository;
    @Mock private StudentStatRepository studentStatRepository;
    @Mock private RoadmapJobService roadmapJobService;
    @Mock private RoadmapProgressService roadmapProgressService;

    private OnboardingLifecycleService service;
    private StudentOnboarding onboarding;

    @BeforeEach
    void setUp() {
        service = new OnboardingLifecycleService(
                userRepository, onboardingRepository, sessionRepository,
                dailyGoalRepository, studentStatRepository, new ObjectMapper(), roadmapJobService,
                roadmapProgressService);
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

    @Test
    void missingGoalAlwaysRoutesToGoalSurveyEvenForLegacyActiveSession() {
        onboarding.setGoalSurveyJson(null);
        onboarding.setPlacementCefrLevel(null);
        when(onboardingRepository.findByStudentIdWithUser(1L)).thenReturn(Optional.of(onboarding));

        OnboardingStatusResponse status = service.getStatus(1L);

        assertThat(status.getNextStep()).isEqualTo("GOAL_SURVEY");
        assertThat(status.getStepNumber()).isEqualTo(1);
        verify(sessionRepository, never())
                .findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(1L);
    }

    @Test
    void changingGoalAfterPlacementIsRejected() {
        GoalSurveyRequest request = goalRequest(LearningGoal.TRAVEL);
        onboarding.setGoalSurveyJson("{\"learningPurpose\":\"WORK\"}");
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));

        assertThatThrownBy(() -> service.submitGoalSurvey(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.GOAL_SURVEY_LOCKED));

        verify(onboardingRepository, never()).save(any());
        verify(roadmapJobService, never()).enqueue(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    void firstGoalForLegacyCompletedPlacementRegeneratesVersionedRoadmap() {
        onboarding.setGoalSurveyJson(null);
        onboarding.setRoadmapGenerationVersion(1);
        onboarding.setRoadmapJson("{\"legacy\":true}");
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.READY);
        GoalSurveyRequest request = goalRequest(LearningGoal.TRAVEL);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));

        service.submitGoalSurvey(1L, request);

        assertThat(onboarding.getRoadmapGenerationVersion()).isEqualTo(2);
        assertThat(onboarding.getRoadmapStatus()).isEqualTo(RoadmapGenerationStatus.PENDING);
        assertThat(onboarding.getRoadmapJson()).isNull();
        verify(roadmapJobService).enqueue(
                org.mockito.ArgumentMatchers.eq(1L),
                org.mockito.ArgumentMatchers.eq(2),
                org.mockito.ArgumentMatchers.eq(CefrLevel.B1),
                contains("TRAVEL"));
    }

    @Test
    void retryingSameGoalAfterPlacementIsIdempotent() throws Exception {
        GoalSurveyRequest request = goalRequest(LearningGoal.WORK);
        onboarding.setGoalSurveyJson(new ObjectMapper().writeValueAsString(request));
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));

        service.submitGoalSurvey(1L, request);

        verify(onboardingRepository, never()).save(any());
        verify(roadmapJobService, never()).enqueue(any(), org.mockito.ArgumentMatchers.anyInt(), any(), any());
    }

    @Test
    void changingGoalWhilePlacementIsActiveIsRejected() {
        GoalSurveyRequest request = goalRequest(LearningGoal.TRAVEL);
        onboarding.setPlacementCefrLevel(null);
        onboarding.setGoalSurveyJson("{\"learningPurpose\":\"WORK\"}");
        PlacementTestSession active = PlacementTestSession.builder()
                .id(10L)
                .student(onboarding.getStudent())
                .isCompleted(false)
                .startedAt(java.time.LocalDateTime.now())
                .lastActivityAt(java.time.LocalDateTime.now())
                .build();
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));
        when(sessionRepository.findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(1L))
                .thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.submitGoalSurvey(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.GOAL_SURVEY_LOCKED));

        verify(onboardingRepository, never()).save(any());
    }

    @Test
    void settingsCannotBeSavedBeforeRoadmapIsReady() {
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.PROCESSING);
        onboarding.setRoadmapJson(null);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));
        OnboardingSettingsRequest request = new OnboardingSettingsRequest((short) 20, LocalTime.of(20, 0));

        assertThatThrownBy(() -> service.saveSettings(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_GENERATING));

        verify(dailyGoalRepository, never()).save(any());
        verify(studentStatRepository, never()).save(any());
    }

    @Test
    void settingsCannotBeSavedBeforePlacementIsComplete() {
        onboarding.setPlacementCefrLevel(null);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));
        OnboardingSettingsRequest request = new OnboardingSettingsRequest((short) 20, LocalTime.of(20, 0));

        assertThatThrownBy(() -> service.saveSettings(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PLACEMENT_TEST_INCOMPLETE));

        verify(dailyGoalRepository, never()).save(any());
        verify(studentStatRepository, never()).save(any());
    }

    @Test
    void settingsExposeRoadmapFailureInsteadOfSkippingTheStep() {
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.FAILED);
        onboarding.setRoadmapJson(null);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));
        OnboardingSettingsRequest request = new OnboardingSettingsRequest((short) 20, LocalTime.of(20, 0));

        assertThatThrownBy(() -> service.saveSettings(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.ROADMAP_GENERATION_FAILED));

        verify(dailyGoalRepository, never()).save(any());
        verify(studentStatRepository, never()).save(any());
    }

    @Test
    void completeOnboardingIsIdempotentAndRepairsUserSummaryFlag() {
        onboarding.setOnboardingCompleted(true);
        onboarding.getStudent().setOnboardingCompleted(false);
        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(onboarding.getStudent()));
        when(onboardingRepository.findByStudentId(1L)).thenReturn(Optional.of(onboarding));

        service.completeOnboarding(1L);

        assertThat(onboarding.getStudent().getOnboardingCompleted()).isTrue();
        verify(userRepository).save(onboarding.getStudent());
        verify(onboardingRepository, never()).save(any());
    }

    private GoalSurveyRequest goalRequest(LearningGoal goal) {
        return new GoalSurveyRequest(
                goal,
                null,
                List.of(LearnerSkill.SPEAKING),
                20,
                "ONLINE",
                "BEGINNER");
    }
}
