package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.config.AdaptiveRecommendationProperties;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationPriority;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationScorerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private RecommendationScorer scorer;

    @BeforeEach
    void setUp() {
        scorer = new RecommendationScorer(new AdaptiveRecommendationProperties());
    }

    @Test
    void urgentAssignmentsAreKeptEvenWhenTheyExceedBudget() {
        TodayPlanCandidate overdue = candidate(
                TodayPlanItemType.ASSIGNMENT, LearnerSkill.SPEAKING, 8,
                RecommendationPriority.P0, 1, 1, 1L);
        TodayPlanCandidate dueSoon = candidate(
                TodayPlanItemType.ASSIGNMENT, LearnerSkill.VOCABULARY, 5,
                RecommendationPriority.P1, 1, 1, 2L);

        List<TodayPlanCandidate> selected = scorer.select(List.of(dueSoon, overdue), 5);

        assertThat(selected).containsExactly(overdue, dueSoon);
        assertThat(selected.stream().mapToInt(TodayPlanCandidate::estimatedMinutes).sum()).isEqualTo(13);
    }

    @Test
    void regularPlanStaysWithinBudgetAndPrefersSkillDiversity() {
        TodayPlanCandidate vocabulary = candidate(
                TodayPlanItemType.VOCABULARY_REVIEW, LearnerSkill.VOCABULARY, 5,
                null, 1, 0.9, 1L);
        TodayPlanCandidate pronunciation = candidate(
                TodayPlanItemType.PRONUNCIATION, LearnerSkill.PRONUNCIATION, 3,
                null, 0.2, 0.8, 2L);
        TodayPlanCandidate vocabularyTopic = candidate(
                TodayPlanItemType.VOCABULARY_TOPIC, LearnerSkill.VOCABULARY, 5,
                null, 0.8, 0.8, 3L);

        List<TodayPlanCandidate> selected = scorer.select(
                List.of(vocabulary, vocabularyTopic, pronunciation), 8);

        assertThat(selected).contains(vocabulary, pronunciation);
        assertThat(selected).doesNotContain(vocabularyTopic);
        assertThat(selected.stream().mapToInt(TodayPlanCandidate::estimatedMinutes).sum())
                .isLessThanOrEqualTo(8);
        assertThat(selected).extracting(TodayPlanCandidate::skill)
                .containsExactlyInAnyOrder(LearnerSkill.VOCABULARY, LearnerSkill.PRONUNCIATION);
    }

    @Test
    void duplicateTargetIsReturnedOnlyOnce() {
        TodayPlanCandidate lower = candidate(
                TodayPlanItemType.PRONUNCIATION, LearnerSkill.PRONUNCIATION, 3,
                null, 0, 0.2, 9L);
        TodayPlanCandidate higher = candidate(
                TodayPlanItemType.PRONUNCIATION, LearnerSkill.PRONUNCIATION, 3,
                null, 0, 0.9, 9L);

        assertThat(scorer.select(List.of(lower, higher), 10)).containsExactly(higher);
    }

    private TodayPlanCandidate candidate(
            TodayPlanItemType type,
            LearnerSkill skill,
            int minutes,
            RecommendationPriority priority,
            double urgency,
            double weakness,
            long targetId) {
        return new TodayPlanCandidate(
                type,
                type.name(),
                skill,
                objectMapper.createObjectNode().put("id", targetId),
                objectMapper.createObjectNode().put("route", type.name()),
                RecommendationReasonCode.WEAK_SKILL,
                objectMapper.createObjectNode(),
                priority,
                minutes,
                0,
                1,
                objectMapper.createObjectNode(),
                urgency,
                weakness,
                0,
                0,
                1,
                priority == null ? null : LocalDateTime.now().plusHours(1));
    }
}
