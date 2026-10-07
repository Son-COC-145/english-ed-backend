package com.example.english_app.dto.response.adaptive;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationPriority;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemStatus;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Value
@Builder
public class TodayPlanItemResponse {
    String recommendationId;
    TodayPlanItemType type;
    String title;
    LearnerSkill skill;
    int estimatedMinutes;
    JsonNode target;
    JsonNode navigation;
    RecommendationReasonCode reasonCode;
    JsonNode reasonParams;
    RecommendationPriority priority;
    TodayPlanItemStatus status;
    int completedUnits;
    int totalUnits;
    BigDecimal progressPercent;
    @JsonProperty("isCompleted")
    boolean completed;
    LocalDateTime completedAt;
}
