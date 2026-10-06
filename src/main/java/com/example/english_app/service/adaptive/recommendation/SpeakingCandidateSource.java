package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.entity.adaptive.LearnerSkillState;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningGoal;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SpeakingCandidateSource implements TodayPlanCandidateSource {

    private final SpeakingSessionRepository speakingSessionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public List<TodayPlanCandidate> collect(TodayPlanContext context) {
        boolean goalFocus = context.survey().focusSkills().contains(LearnerSkill.SPEAKING)
                || context.survey().learningGoal() == LearningGoal.COMMUNICATION;
        boolean weak = context.skillStates().stream()
                .filter(state -> state.getSkill() == LearnerSkill.SPEAKING)
                .map(LearnerSkillState::getMastery)
                .filter(java.util.Objects::nonNull)
                .anyMatch(value -> value.doubleValue() < 60);
        if (!goalFocus && !weak) return List.of();
        if (context.roadmapProgress() == null || context.roadmapProgress().getCurrentWeek() == null) {
            return List.of();
        }

        RoadmapMilestone week = context.roadmapProgress().getMilestones().stream()
                .filter(item -> item.getWeekNumber() == context.roadmapProgress().getCurrentWeek())
                .findFirst()
                .orElse(null);
        if (week == null) return List.of();
        RoadmapModule module = week.getModules().stream()
                .filter(item -> "SPEAKING".equals(item.getType()))
                .filter(item -> !item.getModuleKey().equals(context.roadmapProgress().getCurrentModuleKey()))
                .findFirst()
                .orElse(null);
        if (module == null || module.getContentItemIds().isEmpty()) return List.of();

        List<Short> scenarioIds = module.getContentItemIds().stream().map(Long::shortValue).toList();
        Set<Short> completed = speakingSessionRepository
                .findCompletedScenarioIds(context.studentId(), scenarioIds).stream()
                .collect(Collectors.toSet());
        Short scenarioId = scenarioIds.stream().filter(id -> !completed.contains(id)).findFirst().orElse(null);
        if (scenarioId == null) return List.of();

        RecommendationReasonCode reason = weak
                ? RecommendationReasonCode.WEAK_SKILL
                : RecommendationReasonCode.GOAL_FOCUS;
        return List.of(new TodayPlanCandidate(
                TodayPlanItemType.SPEAKING,
                module.getTitle(),
                LearnerSkill.SPEAKING,
                objectMapper.createObjectNode().put("scenarioId", scenarioId),
                objectMapper.createObjectNode().put("route", "SPEAKING_SESSION")
                        .put("scenarioId", scenarioId),
                reason,
                objectMapper.createObjectNode().put("skill", LearnerSkill.SPEAKING.name()),
                null,
                8,
                0,
                1,
                objectMapper.createObjectNode().put("scenarioId", scenarioId),
                0,
                CandidateSupport.weakness(context, LearnerSkill.SPEAKING),
                1,
                goalFocus ? 1 : 0,
                1,
                null));
    }
}
