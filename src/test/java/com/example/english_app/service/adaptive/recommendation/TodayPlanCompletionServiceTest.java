package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.entity.adaptive.TodayPlanItem;
import com.example.english_app.entity.enums.TodayPlanItemStatus;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.example.english_app.repository.classroom.AssignmentSubmissionRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodayPlanCompletionServiceTest {

    @Mock private StudentVocabularyProgressRepository vocabularyRepository;
    @Mock private PronunciationPracticeLogRepository pronunciationRepository;
    @Mock private SpeakingSessionRepository speakingRepository;
    @Mock private AssignmentSubmissionRepository assignmentRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TodayPlanCompletionService service;

    @BeforeEach
    void setUp() {
        service = new TodayPlanCompletionService(
                vocabularyRepository,
                pronunciationRepository,
                speakingRepository,
                assignmentRepository);
    }

    @Test
    void vocabularySnapshotBecomesPartiallyCompletedFromSourceData() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(10);
        TodayPlanItem item = baseItem(TodayPlanItemType.VOCABULARY_REVIEW, createdAt);
        var snapshot = objectMapper.createObjectNode();
        var ids = snapshot.putArray("vocabularyIds");
        ids.add(10L).add(11L).add(12L);
        item.setSourceSnapshot(snapshot);
        item.setTotalUnits(3);
        when(vocabularyRepository.countPracticedVocabularyIdsSince(
                eq(1L), eq(List.of(10L, 11L, 12L)), eq(createdAt))).thenReturn(2L);

        assertThat(service.reconcile(1L, List.of(item), null, LocalDateTime.now())).isTrue();
        assertThat(item.getStatus()).isEqualTo(TodayPlanItemStatus.IN_PROGRESS);
        assertThat(item.getCompletedUnits()).isEqualTo(2);
        assertThat(item.getCompletedAt()).isNull();
    }

    @Test
    void pronunciationCompletionIsIdempotentAndSetsTimestampOnce() {
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);
        LocalDateTime completedAt = LocalDateTime.now();
        TodayPlanItem item = baseItem(TodayPlanItemType.PRONUNCIATION, createdAt);
        item.setTarget(objectMapper.createObjectNode().put("phonemeId", 7));
        when(pronunciationRepository.countPhonemePracticesSince(
                eq(1L), eq((short) 7), eq(createdAt))).thenReturn(1L);

        assertThat(service.reconcile(1L, List.of(item), null, completedAt)).isTrue();
        assertThat(item.getStatus()).isEqualTo(TodayPlanItemStatus.COMPLETED);
        assertThat(item.getCompletedAt()).isEqualTo(completedAt);
        assertThat(service.reconcile(1L, List.of(item), null, completedAt.plusMinutes(1))).isFalse();
        assertThat(item.getCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void directSourceFallbackDetectsActivityWhenEventWorkerHasNotRun() {
        LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
        when(vocabularyRepository.countByStudentIdAndLastPracticedAtGreaterThanEqual(
                1L, startOfDay)).thenReturn(0L);
        when(pronunciationRepository.countByStudentIdAndPracticedAtGreaterThanEqual(
                1L, startOfDay)).thenReturn(1L);

        assertThat(service.hasLearningActivitySince(1L, startOfDay)).isTrue();
    }

    private TodayPlanItem baseItem(TodayPlanItemType type, LocalDateTime createdAt) {
        return TodayPlanItem.builder()
                .type(type)
                .status(TodayPlanItemStatus.TODO)
                .completedUnits(0)
                .totalUnits(1)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .target(objectMapper.createObjectNode())
                .sourceSnapshot(objectMapper.createObjectNode())
                .build();
    }
}
