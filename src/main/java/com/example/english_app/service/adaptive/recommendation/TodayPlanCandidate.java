package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationPriority;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record TodayPlanCandidate(
        TodayPlanItemType type,
        String title,
        LearnerSkill skill,
        JsonNode target,
        JsonNode navigation,
        RecommendationReasonCode reasonCode,
        JsonNode reasonParams,
        RecommendationPriority priority,
        int estimatedMinutes,
        int completedUnits,
        int totalUnits,
        JsonNode sourceSnapshot,
        double dueUrgency,
        double weakness,
        double roadmapRelevance,
        double goalRelevance,
        double freshness,
        LocalDateTime deadline) {
}
