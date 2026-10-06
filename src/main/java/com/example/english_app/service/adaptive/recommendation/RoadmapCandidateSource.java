package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RoadmapCandidateSource implements TodayPlanCandidateSource {

    private final ObjectMapper objectMapper;

    @Override
    public List<TodayPlanCandidate> collect(TodayPlanContext context) {
        if (context.roadmapProgress() == null
                || context.roadmapProgress().getCurrentModuleKey() == null) {
            return List.of();
        }
        RoadmapModule module = findModule(
                context.roadmapProgress().getMilestones(),
                context.roadmapProgress().getCurrentModuleKey());
        if (module == null || Boolean.TRUE.equals(module.getCompleted())) return List.of();
        LearnerSkill skill = skill(module.getType());
        int estimate = switch (module.getType()) {
            case "SPEAKING" -> 8;
            default -> 5;
        };

        ObjectNode target = objectMapper.createObjectNode()
                .put("roadmapVersion", context.roadmapProgress().getRoadmapVersion())
                .put("moduleKey", module.getModuleKey());
        ObjectNode navigation = objectMapper.createObjectNode()
                .put("route", "ROADMAP_MODULE")
                .put("moduleType", module.getType())
                .put("moduleKey", module.getModuleKey());
        if (module.getTopicId() != null) navigation.put("topicId", module.getTopicId());

        ArrayNode ids = objectMapper.createArrayNode();
        module.getContentItemIds().forEach(ids::add);
        ObjectNode snapshot = objectMapper.createObjectNode();
        snapshot.set("contentItemIds", ids);
        snapshot.put("moduleKey", module.getModuleKey());
        snapshot.put("roadmapVersion", context.roadmapProgress().getRoadmapVersion());

        return List.of(new TodayPlanCandidate(
                TodayPlanItemType.ROADMAP_MODULE,
                module.getTitle(),
                skill,
                target,
                navigation,
                RecommendationReasonCode.ROADMAP_NEXT,
                objectMapper.createObjectNode().put("week", context.roadmapProgress().getCurrentWeek()),
                null,
                estimate,
                module.getDoneCount() == null ? 0 : module.getDoneCount(),
                module.getTotalCount() == null ? module.getItemCount() : module.getTotalCount(),
                snapshot,
                0,
                CandidateSupport.weakness(context, skill),
                1,
                CandidateSupport.goalFocus(context, skill) ? 1 : 0,
                1,
                null));
    }

    private RoadmapModule findModule(List<RoadmapMilestone> milestones, String key) {
        if (milestones == null) return null;
        return milestones.stream()
                .flatMap(week -> week.getModules().stream())
                .filter(module -> key.equals(module.getModuleKey()))
                .findFirst()
                .orElse(null);
    }

    private LearnerSkill skill(String type) {
        return switch (type) {
            case "SPEAKING" -> LearnerSkill.SPEAKING;
            case "IPA_PRONUNCIATION" -> LearnerSkill.PRONUNCIATION;
            default -> LearnerSkill.VOCABULARY;
        };
    }
}
