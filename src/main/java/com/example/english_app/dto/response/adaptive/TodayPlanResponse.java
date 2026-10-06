package com.example.english_app.dto.response.adaptive;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class TodayPlanResponse {
    LocalDate date;
    String timezone;
    int revision;
    LocalDateTime generatedAt;
    LocalDateTime expiresAt;
    long profileVersion;
    String rulesVersion;
    int budgetMinutes;
    int estimatedMinutes;
    int completedMinutes;
    BigDecimal progressPercent;
    @JsonProperty("isCompleted")
    boolean completed;
    String nextRecommendationId;
    String emptyReason;
    List<TodayPlanItemResponse> activities;
}
