package com.example.english_app.service;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.dto.request.OnboardingSettingsRequest;
import com.example.english_app.dto.request.PlacementAnswerRequest;
import com.example.english_app.dto.response.OnboardingStatusResponse;
import com.example.english_app.dto.response.PlacementQuestionResponse;
import com.example.english_app.dto.response.PlacementResultResponse;
import com.example.english_app.dto.response.PronunciationScoreResult;
import com.example.english_app.dto.response.ipa.*;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.*;
import com.example.english_app.entity.gamification.DailyGoal;
import com.example.english_app.entity.gamification.StudentStat;
import com.example.english_app.entity.ipa.*;
import com.example.english_app.entity.onboarding.PlacementTestAnswer;
import com.example.english_app.entity.onboarding.PlacementTestSession;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.question.Question;
import com.example.english_app.entity.user.User;
import com.example.english_app.event.PronunciationCompletedEvent;
import com.example.english_app.repository.gamification.DailyGoalRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.ipa.IpaMinimalPairRepository;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.ipa.PhonemeStatProjection;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.ipa.StudentPhonemeBookmarkRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.question.PlacementTestAnswerRepository;
import com.example.english_app.repository.question.PlacementTestSessionRepository;
import com.example.english_app.repository.question.QuestionRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.audio.AudioAssessmentPort;
import com.example.english_app.service.adaptive.roadmap.RoadmapProgressService;
import com.example.english_app.service.ipa.IpaPronunciationServiceImpl;
import com.example.english_app.service.ipa.IpaServiceImpl;
import com.example.english_app.service.onboarding.OnboardingLifecycleService;
import com.example.english_app.service.onboarding.PlacementResultFactory;
import com.example.english_app.service.onboarding.PlacementTestService;
import com.example.english_app.service.onboarding.PlacementQuestionContentMapper;
import com.example.english_app.service.onboarding.PlacementSessionExpiryService;
import com.example.english_app.service.onboarding.RoadmapJobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Module 0 & Module 1 Full Flow Verification Tests")
class Module0And1FlowTest {

    // ─── Module 0 Dependencies ───────────────────────────────────────────────
    @Mock private UserRepository userRepository;
    @Mock private OnboardingRepository onboardingRepository;
    @Mock private PlacementTestSessionRepository sessionRepository;
    @Mock private PlacementTestAnswerRepository answerRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private DailyGoalRepository dailyGoalRepository;
    @Mock private StudentStatRepository studentStatRepository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();
    @Spy  private PlacementResultFactory resultFactory = new PlacementResultFactory(new ObjectMapper());
    @Mock private PlacementQuestionContentMapper questionContentMapper;
    @Mock private PlacementSessionExpiryService expiryService;
    @Mock private RoadmapJobService roadmapJobService;
    @Mock private RoadmapProgressService roadmapProgressService;

    @InjectMocks private OnboardingLifecycleService lifecycleService;
    @InjectMocks private PlacementTestService placementTestService;

    // ─── Module 1 Dependencies ───────────────────────────────────────────────
    @Mock private IpaPhonemeRepository ipaPhonemeRepository;
    @Mock private StudentPhonemeBookmarkRepository bookmarkRepository;
    @Mock private PronunciationPracticeLogRepository practiceLogRepository;
    @Mock private IpaMinimalPairRepository minimalPairRepository;
    @Mock private com.example.english_app.repository.ipa.IpaPronunciationRuleRepository ruleRepository;
    @Mock private AudioAssessmentPort audioAssessmentPort;
    @Mock private com.example.english_app.repository.ipa.IpaExampleWordRepository exampleWordRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private IpaServiceImpl ipaService;
    @InjectMocks private IpaPronunciationServiceImpl ipaPronunciationService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email("student@test.com")
                .fullName("Nguyễn Văn Học")
                .build();
    }

    // =========================================================================
    // 🚀 MODULE 0: ONBOARDING & PLACEMENT TEST FLOWS
    // =========================================================================
    @Nested
    @DisplayName("Module 0: Onboarding & Placement Test Flows")
    class Module0Flows {

        @Test
        @DisplayName("Luồng 0.1: Khởi đầu Onboarding -> Status ban đầu là GOAL_SURVEY")
        void testInitialOnboardingStatus() {
            // getStatus() dùng findByStudentIdWithUser (JOIN FETCH).
            // Khi trả Optional.empty(), service fallback dùng findUser(userId) để lấy fullName.
            given(onboardingRepository.findByStudentIdWithUser(1L)).willReturn(Optional.empty());
            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));

            OnboardingStatusResponse status = lifecycleService.getStatus(1L);

            assertThat(status.getNextStep()).isEqualTo("GOAL_SURVEY");
            assertThat(status.getStepNumber()).isEqualTo(1);
            assertThat(status.isGoalSurveyCompleted()).isFalse();
            assertThat(status.isPlacementTestCompleted()).isFalse();
        }

        @Test
        @DisplayName("Luồng 0.2: Nộp Goal Survey -> Trạng thái chuyển sang PLACEMENT_TEST")
        void testSubmitGoalSurvey() {
            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(onboardingRepository.findByStudentId(1L)).willReturn(Optional.empty());

            GoalSurveyRequest request = new GoalSurveyRequest();
            request.setLearningPurpose("GIAO_TIEP");
            request.setFocusSkills(List.of("Giao tiếp"));
            request.setDailyStudyMinutes(20);
            request.setPreferredEnvironment("ONLINE");
            request.setPreviousExperience("BEGINNER");

            lifecycleService.submitGoalSurvey(1L, request);

            verify(onboardingRepository).save(argThat(ob ->
                    ob.getGoalSurveyJson() != null && ob.getGoalSurveyJson().contains("GIAO_TIEP")
            ));
        }

        @Test
        @DisplayName("Luồng 0.3: Fast-Track Skip Test (Người mới bắt đầu) -> Gán A1 & Điểm sàn 20 & Sinh Roadmap")
        void testSkipPlacementTest() {
            given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mockUser));
            StudentOnboarding ob = StudentOnboarding.builder().student(mockUser).goalSurveyJson("{}").build();
            given(onboardingRepository.findByStudentId(1L)).willReturn(Optional.of(ob));
            given(sessionRepository.findActiveByStudentIdForUpdate(1L)).willReturn(Optional.empty());

            PlacementResultResponse result = placementTestService.skipTest(1L);

            assertThat(result.getCefrLevel()).isEqualTo("A1");
            assertThat(result.getVocabScore()).isEqualTo((short) 20);
            assertThat(result.getGrammarScore()).isEqualTo((short) 20);
            assertThat(result.getPronunciationScore()).isEqualTo((short) 20);
            assertThat(result.isRoadmapGenerated()).isFalse();
            assertThat(result.getDiagnosticTips()).isNotEmpty();

            verify(onboardingRepository, atLeastOnce()).save(argThat(saved ->
                    saved.getPlacementCefrLevel() == CefrLevel.A1 &&
                    saved.getPlacementVocabScore() == 20
            ));
            verify(roadmapJobService).enqueue(1L, 1, CefrLevel.A1, "{}");
        }

        @Test
        @DisplayName("Luồng 0.4: Xem Tiến độ Lộ trình (Roadmap Progress)")
        void testRoadmapProgressCalculation() throws Exception {
            RoadmapResponse mockRoadmap = RoadmapResponse.builder()
                    .cefrLevel("A1")
                    .totalWeeks(2)
                    .milestones(List.of(
                            RoadmapMilestone.builder().weekNumber(1).title("Tuần 1").modules(List.of(
                                    RoadmapModule.builder().type("VOCABULARY").title("Từ vựng 1").build(),
                                    RoadmapModule.builder().type("IPA_PRONUNCIATION").title("IPA 1").build()
                            )).build(),
                            RoadmapMilestone.builder().weekNumber(2).title("Tuần 2").modules(List.of(
                                    RoadmapModule.builder().type("SPEAKING").title("Giao tiếp 1").build()
                            )).build()
                    ))
                    .build();

            StudentOnboarding ob = StudentOnboarding.builder()
                    .student(mockUser)
                    .roadmapJson(objectMapper.writeValueAsString(mockRoadmap))
                    .build();
            given(onboardingRepository.findByStudentId(1L)).willReturn(Optional.of(ob));

            RoadmapProgressResponse calculated = RoadmapProgressResponse.builder()
                    .cefrLevel("A1")
                    .totalWeeks(2)
                    .currentWeek(1)
                    .totalModules(3)
                    .nextSuggestedModule("Từ vựng 1")
                    .milestones(mockRoadmap.getMilestones())
                    .build();
            given(roadmapProgressService.recalculateAll(ob))
                    .willReturn(new RoadmapProgressService.RoadmapProgressResult(mockRoadmap, calculated));

            RoadmapProgressResponse progress = lifecycleService.getRoadmapProgress(1L);

            assertThat(progress.getCefrLevel()).isEqualTo("A1");
            assertThat(progress.getTotalWeeks()).isEqualTo(2);
            assertThat(progress.getTotalModules()).isEqualTo(3);
            assertThat(progress.getNextSuggestedModule()).isEqualTo("Từ vựng 1");
        }

        @Test
        @DisplayName("Luồng 0.5: Lưu Cài đặt & Hoàn tất Onboarding")
        void testSaveSettingsAndCompleteOnboarding() {
            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mockUser));
            StudentOnboarding ob = StudentOnboarding.builder()
                    .student(mockUser)
                    .goalSurveyJson("{}")
                    .placementCefrLevel(CefrLevel.A1)
                    .dailyGoalXp((short) 20)
                    .roadmapJson("{\"cefrLevel\":\"A1\",\"milestones\":[]}") // P1-B: roadmapJson phải tồn tại
                    .onboardingCompleted(false)
                    .build();
            given(onboardingRepository.findByStudentId(1L)).willReturn(Optional.of(ob));
            given(dailyGoalRepository.existsByStudentIdAndGoalDate(eq(1L), any(LocalDate.class))).willReturn(false);
            given(studentStatRepository.existsById(1L)).willReturn(false);

            OnboardingSettingsRequest settingsReq = new OnboardingSettingsRequest();
            settingsReq.setDailyGoalXp((short) 30);
            settingsReq.setReminderTime(LocalTime.of(20, 0));

            lifecycleService.saveSettings(1L, settingsReq);
            verify(dailyGoalRepository).save(any(DailyGoal.class));
            verify(studentStatRepository).save(any(StudentStat.class));

            lifecycleService.completeOnboarding(1L);
            assertThat(ob.getOnboardingCompleted()).isTrue();
            assertThat(ob.getOnboardingCompletedAt()).isNotNull();
            assertThat(mockUser.getOnboardingCompleted()).isTrue();
        }

        @Test
        @DisplayName("Luồng 0.6: getStatus trả về đúng isPlacementSkipped và SKIPPED status")
        void testOnboardingStatus_WhenPlacementSkipped() {
            StudentOnboarding ob = StudentOnboarding.builder()
                    .student(mockUser)
                    .goalSurveyJson("{\"learningPurpose\":\"WORK\"}")
                    .placementCefrLevel(CefrLevel.A1)
                    .isPlacementSkipped(true)
                    .dailyGoalXp((short) 20)
                    .onboardingCompleted(false)
                    .build();

            given(onboardingRepository.findByStudentIdWithUser(1L)).willReturn(Optional.of(ob));
            given(sessionRepository.findTopByStudentIdAndIsCompletedFalseOrderByStartedAtDesc(1L)).willReturn(Optional.empty());

            OnboardingStatusResponse status = lifecycleService.getStatus(1L);

            assertThat(status.isPlacementSkipped()).isTrue();
            assertThat(status.getPlacementTestStatus()).isEqualTo("SKIPPED");
            assertThat(status.isPlacementTestCompleted()).isTrue();
        }

        @Test
        @DisplayName("Luồng 0.7: resetOnboarding phải reset cả goalSurveyJson và isPlacementSkipped")
        void testResetOnboarding_ShouldResetGoalSurveyAndSkippedState() {
            StudentOnboarding ob = StudentOnboarding.builder()
                    .student(mockUser)
                    .goalSurveyJson("{\"learningPurpose\":\"WORK\"}")
                    .placementCefrLevel(CefrLevel.B1)
                    .placementVocabScore((short) 70)
                    .isPlacementSkipped(true)
                    .roadmapJson("{\"milestones\":[]}")
                    .roadmapGenerationVersion(3)
                    .onboardingCompleted(true)
                    .build();

            given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(mockUser));
            given(onboardingRepository.findByStudentId(1L)).willReturn(Optional.of(ob));
            given(sessionRepository.findActiveByStudentIdForUpdate(1L)).willReturn(Optional.empty());

            lifecycleService.resetOnboarding(1L);

            assertThat(ob.getGoalSurveyJson()).isNull();
            assertThat(ob.getIsPlacementSkipped()).isFalse();
            assertThat(ob.getPlacementCefrLevel()).isNull();
            assertThat(ob.getPlacementVocabScore()).isNull();
            assertThat(ob.getRoadmapJson()).isNull();
            assertThat(ob.getRoadmapGenerationVersion()).isEqualTo(4);
            assertThat(ob.getOnboardingCompleted()).isFalse();
            assertThat(mockUser.getOnboardingCompleted()).isFalse();
            verify(onboardingRepository).save(ob);
        }
    }

    // =========================================================================
    // 🎙️ MODULE 1: IPA SOUND LIBRARY & AI ASSESSMENT FLOWS
    // =========================================================================
    @Nested
    @DisplayName("Module 1: IPA Sound Library & AI Assessment Flows")
    class Module1Flows {

        private IpaPhoneme phonemeSh;
        private IpaPhoneme phonemeS;

        @BeforeEach
        void initPhonemes() {
            phonemeSh = IpaPhoneme.builder()
                    .id((short) 1)
                    .symbol("ʃ")
                    .phonemeType(PhonemeType.CONSONANT)
                    .nameVi("Âm sh nặng")
                    .audioMaleUrl("https://cdn/sh-male.mp3")
                    .audioFemaleUrl("https://cdn/sh-female.mp3")
                    .videoMouthUrl("https://cdn/sh-mouth.mp4")
                    .pronunciationTipVi("Chu môi tròn về phía trước, đẩy hơi mạnh")
                    .cefrIntroLevel(CefrLevel.A1)
                    .isCommonVnError(true)
                    .exampleWords(List.of(
                            IpaExampleWord.builder().id(10L).word("she").ipaTranscription("/ʃiː/").audioUrl("https://cdn/she.mp3").build()
                    ))
                    .build();

            phonemeS = IpaPhoneme.builder()
                    .id((short) 2)
                    .symbol("s")
                    .phonemeType(PhonemeType.CONSONANT)
                    .nameVi("Âm s nhẹ")
                    .audioMaleUrl("https://cdn/s-male.mp3")
                    .audioFemaleUrl("https://cdn/s-female.mp3")
                    .cefrIntroLevel(CefrLevel.A1)
                    .build();
        }

        @Test
        @DisplayName("Luồng 1.1: Xem chi tiết âm IPA kèm Lời khuyên khẩu hình tiếng Việt")
        void testGetPhonemeDetailWithVietnameseTip() {
            given(ipaPhonemeRepository.findByIdWithWords((short) 1)).willReturn(Optional.of(phonemeSh));

            IpaPhonemeDetailResponse detail = ipaService.getPhonemeDetail((short) 1);

            assertThat(detail.symbol()).isEqualTo("ʃ");
            assertThat(detail.pronunciationTipVi()).isEqualTo("Chu môi tròn về phía trước, đẩy hơi mạnh");
            assertThat(detail.exampleWords()).hasSize(1);
            assertThat(detail.exampleWords().get(0).getWord()).isEqualTo("she");
        }

        @Test
        @DisplayName("Luồng 1.2: Lấy Bảng nhiệt độ thành thạo 44 âm (Mastery Heatmap)")
        void testGetPhonemeMasteryHeatmap() {
            given(ipaPhonemeRepository.findAll()).willReturn(List.of(phonemeSh, phonemeS));

            PhonemeStatProjection statSh = mock(PhonemeStatProjection.class);
            given(statSh.getPhonemeId()).willReturn((short) 1);
            given(statSh.getAvgScore()).willReturn(88.5);
            given(statSh.getMaxScore()).willReturn((short) 95);
            given(statSh.getPracticeCount()).willReturn(5L);

            given(practiceLogRepository.findPhonemeStatsByStudentId(1L)).willReturn(List.of(statSh));

            IpaMasterySummaryResponse mastery = ipaService.getPhonemeMastery(1L);

            assertThat(mastery.getTotalPhonemes()).isEqualTo(2);
            assertThat(mastery.getMasteredCount()).isEqualTo(1); // /ʃ/ có avg 88.5 >= 80 -> MASTERED
            assertThat(mastery.getUnlearnedCount()).isEqualTo(1); // /s/ chưa học -> UNLEARNED
            assertThat(mastery.getOverallMasteryPercent()).isEqualTo(50.0);

            IpaPhonemeMasteryDto dtoSh = mastery.getPhonemes().stream().filter(p -> p.getId() == 1).findFirst().orElseThrow();
            assertThat(dtoSh.getMasteryStatus()).isEqualTo(IpaMasteryStatus.MASTERED);
            assertThat(dtoSh.getAverageScore()).isEqualTo((short) 89);
        }

        @Test
        @DisplayName("Luồng 1.3: Lấy danh sách Cặp âm dễ nhầm lẫn (Minimal Pairs)")
        void testGetMinimalPairs() {
            IpaMinimalPair pair = IpaMinimalPair.builder()
                    .id(1L)
                    .title("Phân biệt /s/ và /ʃ/")
                    .description("So sánh s nhẹ và sh nặng")
                    .phoneme1(phonemeS)
                    .word1("see")
                    .ipa1("/siː/")
                    .phoneme2(phonemeSh)
                    .word2("she")
                    .ipa2("/ʃiː/")
                    .isActive(true)
                    .build();

            given(minimalPairRepository.findAllActiveWithPhonemes()).willReturn(List.of(pair));

            List<IpaMinimalPairResponse> pairs = ipaService.getMinimalPairs();

            assertThat(pairs).hasSize(1);
            assertThat(pairs.get(0).getTitle()).isEqualTo("Phân biệt /s/ và /ʃ/");
            assertThat(pairs.get(0).getWord1()).isEqualTo("see");
            assertThat(pairs.get(0).getWord2()).isEqualTo("she");
        }

        @Test
        @DisplayName("Luồng 1.4: Lấy Lịch sử luyện tập gần nhất của 1 âm")
        void testGetPracticeHistory() {
            PronunciationPracticeLog logEntry = PronunciationPracticeLog.builder()
                    .id(101L)
                    .overallScore((short) 85)
                    .fluencyScore((short) 85)
                    .completenessScore((short) 85)
                    .stressCorrect(true)
                    .practicedAt(LocalDateTime.now())
                    .build();

            Object[] rawRow = new Object[]{logEntry, "she", "/ʃiː/"};
            List<Object[]> rawList = Collections.singletonList(rawRow);
            doReturn(rawList).when(practiceLogRepository).findHistoryByStudentAndPhonemeRaw(eq(1L), eq((short) 1), any());

            List<IpaPracticeHistoryResponse> history = ipaService.getPracticeHistory(1L, (short) 1);

            assertThat(history).hasSize(1);
            assertThat(history.get(0).getWord()).isEqualTo("she");
            assertThat(history.get(0).getOverallScore()).isEqualTo((short) 85);
            assertThat(history.get(0).getScoreColor()).isEqualTo("GREEN");
        }

        @Test
        @DisplayName("Luồng 1.5: Luyện tập phát âm từ ví dụ -> Chấm điểm AI & Dispatch Gamification Event")
        void testAssessPronunciationAndDispatchEvent() {
            IpaExampleWord exampleWord = IpaExampleWord.builder()
                    .id(10L)
                    .word("she")
                    .ipaTranscription("/ʃiː/")
                    .phoneme(phonemeSh)
                    .build();

            given(exampleWordRepository.findById(10L)).willReturn(Optional.of(exampleWord));
            given(userRepository.getReferenceById(1L)).willReturn(mockUser);

            PronunciationScoreResult azureResult = PronunciationScoreResult.builder()
                    .word("she")
                    .overallScore((short) 90)
                    .accuracyScore((short) 90)
                    .fluencyScore((short) 90)
                    .completenessScore((short) 90)
                    .scoreColor("GREEN")
                    .status("SCORED")
                    .build();

            given(audioAssessmentPort.assess(any(byte[].class), eq("she"))).willReturn(azureResult);

            PronunciationPracticeLog savedLog = PronunciationPracticeLog.builder()
                    .id(500L)
                    .overallScore((short) 90)
                    .build();
            given(practiceLogRepository.save(any(PronunciationPracticeLog.class))).willReturn(savedLog);

            MockMultipartFile mockAudio = new MockMultipartFile(
                    "audio", "test.wav", "audio/wav", new byte[]{1, 2, 3, 4}
            );

            PronunciationResultResponse response = ipaPronunciationService.assess(1L, 10L, mockAudio);

            assertThat(response.overallScore()).isEqualTo((short) 90);
            assertThat(response.phonemes()).hasSize(1);
            assertThat(response.phonemes().get(0).color()).isEqualTo("GREEN");

            // Kiểm tra event cộng XP được gửi đi
            ArgumentCaptor<PronunciationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(PronunciationCompletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            PronunciationCompletedEvent publishedEvent = eventCaptor.getValue();

            assertThat(publishedEvent.getStudentId()).isEqualTo(1L);
            assertThat(publishedEvent.getXpReward()).isEqualTo(20); // Điểm >= 80 -> 20 XP
            assertThat(publishedEvent.getLogId()).isEqualTo(500L);
        }

        @Test
        @DisplayName("Luồng 1.6: Lấy Danh sách & Chi tiết Quy tắc Trọng âm / Ghép âm (Pronunciation Rules)")
        void testGetPronunciationRulesAndDetail() {
            IpaPronunciationRule rule = IpaPronunciationRule.builder()
                    .id(1L)
                    .category(PronunciationRuleCategory.WORD_STRESS)
                    .titleVi("Quy tắc trọng âm từ 2 âm tiết")
                    .summaryVi("Danh từ nhấn âm 1, Động từ nhấn âm 2")
                    .contentMarkdown("### Chi tiết quy tắc trọng âm...")
                    .examplesJson("[{\"word\":\"Doctor\",\"ipa\":\"/ˈdɒktə/\",\"meaning\":\"Bác sĩ\",\"audioUrl\":\"https://cdn/doc.mp3\"}]")
                    .orderIndex(1)
                    .isActive(true)
                    .build();

            given(ruleRepository.findByCategory(PronunciationRuleCategory.WORD_STRESS)).willReturn(List.of(rule));
            given(ruleRepository.findById(1L)).willReturn(Optional.of(rule));

            List<PronunciationRuleResponse> rules = ipaService.getPronunciationRules(PronunciationRuleCategory.WORD_STRESS);
            assertThat(rules).hasSize(1);
            assertThat(rules.get(0).getTitleVi()).isEqualTo("Quy tắc trọng âm từ 2 âm tiết");

            PronunciationRuleDetailResponse detail = ipaService.getPronunciationRuleDetail(1L);
            assertThat(detail.getId()).isEqualTo(1L);
            assertThat(detail.getCategory()).isEqualTo(PronunciationRuleCategory.WORD_STRESS);
            assertThat(detail.getExamples()).hasSize(1);
            assertThat(detail.getExamples().get(0).getWord()).isEqualTo("Doctor");
        }
    }
}
