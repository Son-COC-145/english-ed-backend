package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.adaptive.RoadmapModuleProgress;
import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.example.english_app.entity.enums.RoadmapModuleStatus;
import com.example.english_app.entity.enums.RoadmapUnlockReason;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapProgressServiceTest {

    @Mock private OnboardingRepository onboardingRepository;
    @Mock private RoadmapContentResolver contentResolver;
    @Mock private RoadmapModuleProgressRepository progressRepository;
    @Mock private RoadmapProgressBatchWriter progressBatchWriter;
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
                progressBatchWriter,
                vocabularyProgressRepository,
                speakingSessionRepository,
                pronunciationPracticeLogRepository);
    }

    @Test
    void calculatesWeightedProgressWithBatchedSourceQueries() {
        RoadmapModule vocabulary = module("VOCABULARY", "VOCABULARY:1", List.of(1L, 2L));
        RoadmapModule ipa = module("IPA_PRONUNCIATION", "IPA:BASIC", List.of(3L, 4L));
        RoadmapModule speaking = module("SPEAKING", "SPEAKING:2", List.of(5L));
        RoadmapResponse roadmap = roadmap(List.of(
                RoadmapMilestone.builder().weekNumber(1).modules(List.of(vocabulary, ipa)).build(),
                RoadmapMilestone.builder().weekNumber(2).modules(List.of(speaking)).build()));
        StudentOnboarding onboarding = onboarding(roadmap, 3);
        when(contentResolver.resolve(onboarding)).thenReturn(roadmap);
        when(vocabularyProgressRepository.findPracticedVocabularyIds(10L, List.of(1L, 2L)))
                .thenReturn(List.of(1L, 2L));
        when(pronunciationPracticeLogRepository.findPracticedPhonemeIds(
                10L, List.of((short) 3, (short) 4))).thenReturn(List.of((short) 3));
        when(speakingSessionRepository.findCompletedScenarioIds(10L, List.of((short) 5)))
                .thenReturn(List.of((short) 5));
        when(progressRepository.findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(
                10L, 3)).thenReturn(List.of(
                        progress("VOCABULARY:1", 1, 0, 2, 2, RoadmapModuleStatus.COMPLETED),
                        progress("IPA:BASIC", 1, 1, 1, 2, RoadmapModuleStatus.IN_PROGRESS),
                        progress("SPEAKING:2", 2, 0, 1, 1, RoadmapModuleStatus.COMPLETED)));

        RoadmapProgressResponse result = service.recalculateAll(onboarding).progress();

        assertThat(result.getRoadmapVersion()).isEqualTo(3);
        assertThat(result.getCurrentWeek()).isEqualTo(1);
        assertThat(result.getCurrentModuleKey()).isEqualTo("IPA:BASIC");
        assertThat(result.getCompletedWeeks()).isEqualTo(1);
        assertThat(result.getCompletedModules()).isEqualTo(2);
        assertThat(result.getPercentCompleted()).isEqualTo(80.0);
        assertThat(ipa.getStatus()).isEqualTo(RoadmapItemStatus.IN_PROGRESS);
        assertThat(ipa.getProgressPercent()).isEqualTo(50.0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<RoadmapProgressBatchWriter.ProgressUpdate>> updates =
                ArgumentCaptor.forClass(List.class);
        verify(progressBatchWriter).upsertAll(updates.capture());
        assertThat(updates.getValue()).hasSize(3);
    }

    @Test
    void exposesAllModulesInCurrentWeekAndLocksFutureWeeks() {
        RoadmapModule vocabulary = module("VOCABULARY", "VOCABULARY:1", List.of(1L));
        RoadmapModule ipa = module("IPA_PRONUNCIATION", "IPA:BASIC", List.of(2L));
        RoadmapModule speaking = module("SPEAKING", "SPEAKING:2", List.of(3L));
        RoadmapResponse roadmap = roadmap(List.of(
                RoadmapMilestone.builder().weekNumber(1).modules(List.of(vocabulary, ipa)).build(),
                RoadmapMilestone.builder().weekNumber(2).modules(List.of(speaking)).build()));
        StudentOnboarding onboarding = onboarding(roadmap, 4);
        when(contentResolver.resolve(onboarding)).thenReturn(roadmap);
        when(vocabularyProgressRepository.findPracticedVocabularyIds(10L, List.of(1L)))
                .thenReturn(List.of());
        when(pronunciationPracticeLogRepository.findPracticedPhonemeIds(10L, List.of((short) 2)))
                .thenReturn(List.of());
        when(speakingSessionRepository.findCompletedScenarioIds(10L, List.of((short) 3)))
                .thenReturn(List.of());
        when(progressRepository.findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(
                10L, 4)).thenReturn(List.of(
                        progress("VOCABULARY:1", 1, 0, 0, 1, RoadmapModuleStatus.NOT_STARTED),
                        progress("IPA:BASIC", 1, 1, 0, 1, RoadmapModuleStatus.NOT_STARTED),
                        progress("SPEAKING:2", 2, 0, 0, 1, RoadmapModuleStatus.NOT_STARTED)));

        RoadmapProgressResponse result = service.recalculateAll(onboarding).progress();

        assertThat(result.getCurrentModuleKey()).isEqualTo("VOCABULARY:1");
        assertThat(vocabulary.getStatus()).isEqualTo(RoadmapItemStatus.AVAILABLE);
        assertThat(ipa.getStatus()).isEqualTo(RoadmapItemStatus.AVAILABLE);
        assertThat(speaking.getStatus()).isEqualTo(RoadmapItemStatus.LOCKED);
        assertThat(speaking.getUnlockCondition().getReasonCode())
                .isEqualTo(RoadmapUnlockReason.PREVIOUS_WEEK_REQUIRED);
        assertThat(speaking.getUnlockCondition().getPrerequisiteWeek()).isEqualTo(1);
        assertThat(result.isCompleted()).isFalse();
        assertThat(result.getPercentCompleted()).isZero();
    }

    @Test
    void rejectsEmptyModuleInsteadOfReportingFalseCompletion() {
        RoadmapModule empty = module("VOCABULARY", "VOCABULARY:9", List.of());
        RoadmapResponse roadmap = roadmap(List.of(
                RoadmapMilestone.builder().weekNumber(1).modules(List.of(empty)).build()));
        StudentOnboarding onboarding = onboarding(roadmap, 1);
        when(contentResolver.resolve(onboarding)).thenReturn(roadmap);

        assertThatThrownBy(() -> service.recalculateAll(onboarding))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("has no content");
        verify(progressBatchWriter, never()).upsertAll(any());
    }

    @Test
    void rejectsEmptyRoadmapInsteadOfReportingAvailable() {
        RoadmapResponse roadmap = roadmap(List.of());
        StudentOnboarding onboarding = onboarding(roadmap, 1);
        when(contentResolver.resolve(onboarding)).thenReturn(roadmap);

        assertThatThrownBy(() -> service.recalculateAll(onboarding))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("no milestones");
        verify(progressBatchWriter, never()).upsertAll(any());
    }

    @Test
    void reportsCompletedRoadmapWithoutCurrentPointers() {
        RoadmapModule vocabulary = module("VOCABULARY", "VOCABULARY:1", List.of(1L));
        RoadmapModule speaking = module("SPEAKING", "SPEAKING:2", List.of(2L));
        RoadmapResponse roadmap = roadmap(List.of(
                RoadmapMilestone.builder().weekNumber(1).modules(List.of(vocabulary)).build(),
                RoadmapMilestone.builder().weekNumber(2).modules(List.of(speaking)).build()));
        StudentOnboarding onboarding = onboarding(roadmap, 5);
        when(contentResolver.resolve(onboarding)).thenReturn(roadmap);
        when(vocabularyProgressRepository.findPracticedVocabularyIds(10L, List.of(1L)))
                .thenReturn(List.of(1L));
        when(speakingSessionRepository.findCompletedScenarioIds(10L, List.of((short) 2)))
                .thenReturn(List.of((short) 2));
        when(progressRepository.findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(
                10L, 5)).thenReturn(List.of(
                        progress("VOCABULARY:1", 1, 0, 1, 1, RoadmapModuleStatus.COMPLETED),
                        progress("SPEAKING:2", 2, 0, 1, 1, RoadmapModuleStatus.COMPLETED)));

        RoadmapProgressResponse result = service.recalculateAll(onboarding).progress();

        assertThat(result.getStatus()).isEqualTo(RoadmapItemStatus.COMPLETED);
        assertThat(result.isCompleted()).isTrue();
        assertThat(result.getCompletedWeeks()).isEqualTo(2);
        assertThat(result.getPercentCompleted()).isEqualTo(100.0);
        assertThat(result.getCurrentWeek()).isNull();
        assertThat(result.getCurrentModuleKey()).isNull();
        assertThat(result.getNextSuggestedModule()).isNull();
    }

    private RoadmapResponse roadmap(List<RoadmapMilestone> milestones) {
        return RoadmapResponse.builder()
                .schemaVersion(2)
                .currentCefrLevel("A2")
                .targetCefrLevel("B1")
                .cefrLevel("A2")
                .totalWeeks(milestones.size())
                .milestones(milestones)
                .build();
    }

    private StudentOnboarding onboarding(RoadmapResponse roadmap, int version) {
        return StudentOnboarding.builder()
                .student(User.builder().id(10L).build())
                .roadmapJson("present")
                .roadmapGenerationVersion(version)
                .build();
    }

    private RoadmapModule module(String type, String key, List<Long> ids) {
        return RoadmapModule.builder()
                .type(type)
                .title(key)
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
