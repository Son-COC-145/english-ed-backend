package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.adaptive.RoadmapModuleProgress;
import com.example.english_app.entity.enums.RoadmapModuleStatus;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.entity.user.User;
import com.example.english_app.repository.adaptive.RoadmapModuleProgressRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapProgressServiceTest {

    @Mock private OnboardingRepository onboardingRepository;
    @Mock private RoadmapContentResolver contentResolver;
    @Mock private RoadmapModuleProgressRepository progressRepository;
    @Mock private StudentVocabularyProgressRepository vocabularyProgressRepository;
    @Mock private SpeakingSessionRepository speakingSessionRepository;
    @Mock private PronunciationPracticeLogRepository pronunciationPracticeLogRepository;

    private RoadmapProgressService service;

    @BeforeEach
    void setUp() {
        service = new RoadmapProgressService(
                onboardingRepository,
                contentResolver,
                progressRepository,
                vocabularyProgressRepository,
                speakingSessionRepository,
                pronunciationPracticeLogRepository);
        when(progressRepository.upsert(
                anyLong(), anyInt(), anyInt(), anyInt(), anyString(), anyString(),
                any(), anyInt(), anyInt(), anyString(), any(), any(LocalDateTime.class)))
                .thenReturn(1);
    }

    @Test
    void calculatesRealCountsCurrentWeekAndNextModule() {
        RoadmapModule vocabulary = module("VOCABULARY", "VOCABULARY:1", "Từ vựng", List.of(1L, 2L));
        RoadmapModule ipa = module("IPA_PRONUNCIATION", "IPA:IPA_VOWELS_BASIC", "Nguyên âm", List.of(3L, 4L));
        RoadmapModule speaking = module("SPEAKING", "SPEAKING:2", "Hội thoại", List.of(5L));
        RoadmapResponse roadmap = RoadmapResponse.builder()
                .cefrLevel("A2")
                .totalWeeks(2)
                .milestones(List.of(
                        RoadmapMilestone.builder().weekNumber(1).modules(List.of(vocabulary, ipa)).build(),
                        RoadmapMilestone.builder().weekNumber(2).modules(List.of(speaking)).build()))
                .build();
        StudentOnboarding onboarding = onboarding(roadmap, 3);
        when(contentResolver.resolve(onboarding)).thenReturn(roadmap);
        when(vocabularyProgressRepository.countPracticedVocabularyIds(10L, List.of(1L, 2L))).thenReturn(2L);
        when(pronunciationPracticeLogRepository.countPracticedPhonemeIds(10L, List.of((short) 3, (short) 4)))
                .thenReturn(1L);
        when(speakingSessionRepository.countCompletedScenarioIds(10L, List.of((short) 5))).thenReturn(1L);
        when(progressRepository.findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(10L, 3))
                .thenReturn(List.of(
                        progress("VOCABULARY:1", 1, 0, 2, 2, RoadmapModuleStatus.COMPLETED),
                        progress("IPA:IPA_VOWELS_BASIC", 1, 1, 1, 2, RoadmapModuleStatus.IN_PROGRESS),
                        progress("SPEAKING:2", 2, 0, 1, 1, RoadmapModuleStatus.COMPLETED)));

        RoadmapProgressResponse result = service.recalculateAll(onboarding).progress();

        assertThat(result.getRoadmapVersion()).isEqualTo(3);
        assertThat(result.getCurrentWeek()).isEqualTo(1);
        assertThat(result.getCompletedWeeks()).isEqualTo(1);
        assertThat(result.getCompletedModules()).isEqualTo(2);
        assertThat(result.getPercentCompleted()).isEqualTo(66.7);
        assertThat(result.getNextSuggestedModule()).isEqualTo("Nguyên âm");
        assertThat(ipa.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(ipa.getProgressPercent()).isEqualTo(50.0);
    }

    @Test
    void emptyModuleIsCompletedAndCannotBlockTheWeek() {
        RoadmapModule empty = module("VOCABULARY", "VOCABULARY:9", "Chủ đề trống", List.of());
        RoadmapResponse roadmap = RoadmapResponse.builder()
                .cefrLevel("A1")
                .totalWeeks(1)
                .milestones(List.of(RoadmapMilestone.builder()
                        .weekNumber(1).modules(List.of(empty)).build()))
                .build();
        StudentOnboarding onboarding = onboarding(roadmap, 1);
        when(contentResolver.resolve(onboarding)).thenReturn(roadmap);
        when(progressRepository.findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(10L, 1))
                .thenReturn(List.of(progress(
                        "VOCABULARY:9", 1, 0, 0, 0, RoadmapModuleStatus.COMPLETED)));

        RoadmapProgressResponse result = service.recalculateAll(onboarding).progress();

        assertThat(result.getCompletedWeeks()).isEqualTo(1);
        assertThat(result.getCompletedModules()).isEqualTo(1);
        assertThat(result.getPercentCompleted()).isEqualTo(100.0);
        assertThat(empty.getProgressPercent()).isEqualTo(100.0);
        verify(progressRepository).upsert(
                eq(10L), eq(1), eq(1), eq(0), eq("VOCABULARY:9"), eq("VOCABULARY"),
                any(), eq(0), eq(0), eq("COMPLETED"), any(LocalDateTime.class), any(LocalDateTime.class));
    }

    private StudentOnboarding onboarding(RoadmapResponse roadmap, int version) {
        return StudentOnboarding.builder()
                .student(User.builder().id(10L).build())
                .roadmapJson("present")
                .roadmapGenerationVersion(version)
                .build();
    }

    private RoadmapModule module(String type, String key, String title, List<Long> ids) {
        return RoadmapModule.builder()
                .type(type)
                .title(title)
                .moduleKey(key)
                .contentItemIds(ids)
                .contentVersion("version")
                .itemCount(ids.size())
                .build();
    }

    private RoadmapModuleProgress progress(
            String key,
            int week,
            int index,
            int done,
            int total,
            RoadmapModuleStatus status) {
        return RoadmapModuleProgress.builder()
                .moduleKey(key)
                .weekNumber(week)
                .moduleIndex(index)
                .doneCount(done)
                .totalCount(total)
                .status(status)
                .build();
    }
}
