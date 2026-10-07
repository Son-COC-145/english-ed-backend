package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.config.AdaptiveRecommendationProperties;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.adaptive.LearnerProfile;
import com.example.english_app.entity.adaptive.TodayPlan;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemStatus;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.adaptive.LearnerSkillStateRepository;
import com.example.english_app.repository.adaptive.StudentDailyActivityRepository;
import com.example.english_app.repository.adaptive.TodayPlanItemRepository;
import com.example.english_app.repository.adaptive.TodayPlanRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.adaptive.learner.LearnerProfileService;
import com.example.english_app.service.adaptive.roadmap.RoadmapProgressService;
import com.example.english_app.service.onboarding.GoalSurveyParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodayPlanServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OnboardingRepository onboardingRepository;
    @Mock private LearnerProfileService learnerProfileService;
    @Mock private LearnerSkillStateRepository skillRepository;
    @Mock private RoadmapProgressService roadmapProgressService;
    @Mock private TodayPlanCompletionService completionService;
    @Mock private TodayPlanRepository planRepository;
    @Mock private TodayPlanItemRepository itemRepository;
    @Mock private StudentStatRepository statRepository;
    @Mock private StudentDailyActivityRepository dailyActivityRepository;
    @Mock private GoalSurveyParser goalSurveyParser;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TodayPlanService service;
    private TodayPlanCandidate candidate;

    @BeforeEach
    void setUp() {
        AdaptiveRecommendationProperties properties = new AdaptiveRecommendationProperties();
        candidate = new TodayPlanCandidate(
                TodayPlanItemType.PRONUNCIATION,
                "Practice /i:/",
                LearnerSkill.PRONUNCIATION,
                objectMapper.createObjectNode().put("phonemeId", 7),
                objectMapper.createObjectNode().put("route", "IPA_PHONEME").put("phonemeId", 7),
                RecommendationReasonCode.WEAK_PHONEME,
                objectMapper.createObjectNode().put("avgScore", 45),
                null,
                3,
                0,
                1,
                objectMapper.createObjectNode().put("phonemeId", 7),
                0,
                0.55,
                0,
                1,
                1,
                null);
        TodayPlanCandidateSource source = context -> List.of(candidate);
        service = new TodayPlanService(
                userRepository,
                onboardingRepository,
                learnerProfileService,
                skillRepository,
                roadmapProgressService,
                List.of(source),
                new RecommendationScorer(properties),
                completionService,
                planRepository,
                itemRepository,
                statRepository,
                dailyActivityRepository,
                goalSurveyParser,
                properties,
                objectMapper);
    }

    @Test
    void createsPersistedPlanWithStableServerOwnedProgressContract() {
        User user = User.builder().id(1L).build();
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .student(user)
                .roadmapJson("{}")
                .build();
        LearnerProfile profile = LearnerProfile.builder()
                .studentId(1L)
                .profileVersion(3L)
                .build();
        RoadmapProgressResponse progress = RoadmapProgressResponse.builder().milestones(List.of()).build();

        when(userRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(user));
        when(onboardingRepository.findByStudentIdWithUser(1L)).thenReturn(Optional.of(onboarding));
        when(goalSurveyParser.parse(any())).thenReturn(new GoalSurveyParser.ParsedGoalSurvey(
                com.example.english_app.entity.enums.LearningGoal.GENERAL,
                List.of(),
                List.of(LearnerSkill.PRONUNCIATION),
                15));
        when(learnerProfileService.ensureProfile(1L)).thenReturn(profile);
        when(skillRepository.findByStudentIdOrderBySkillAsc(1L)).thenReturn(List.of());
        when(roadmapProgressService.recalculateAll(onboarding)).thenReturn(
                new RoadmapProgressService.RoadmapProgressResult(new RoadmapResponse(), progress));
        when(planRepository.findForUpdate(any(), any(), any())).thenReturn(Optional.empty());
        when(planRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            TodayPlan plan = invocation.getArgument(0);
            plan.setId(99L);
            return plan;
        });
        when(statRepository.findByStudentId(1L)).thenReturn(Optional.empty());
        when(dailyActivityRepository.findByStudentIdAndActivityDate(any(), any()))
                .thenReturn(Optional.empty());
        when(completionService.hasLearningActivitySince(any(), any())).thenReturn(false);

        var response = service.getTodayPlan(1L, null);

        assertThat(response.getRevision()).isEqualTo(1);
        assertThat(response.getBudgetMinutes()).isEqualTo(15);
        assertThat(response.getEstimatedMinutes()).isEqualTo(3);
        assertThat(response.getCompletedMinutes()).isZero();
        assertThat(response.isCompleted()).isFalse();
        assertThat(response.getNextRecommendationId()).hasSize(64);
        assertThat(response.getActivities()).singleElement().satisfies(item -> {
            assertThat(item.getRecommendationId()).isEqualTo(response.getNextRecommendationId());
            assertThat(item.getStatus()).isEqualTo(TodayPlanItemStatus.TODO);
            assertThat(item.getProgressPercent()).isEqualByComparingTo("0.00");
            assertThat(item.getNavigation().path("route").asText()).isEqualTo("IPA_PHONEME");
        });
    }
}
