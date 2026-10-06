package com.example.english_app.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "adaptive.recommendation")
public class AdaptiveRecommendationProperties {

    @NotBlank
    private String rulesVersion = "2026-10-06.1";

    @Min(5)
    @Max(120)
    private int defaultBudgetMinutes = 15;

    @Min(0)
    @Max(100)
    private int weakPhonemeThreshold = 60;

    @Min(1)
    private int assignmentHorizonDays = 3;

    @Valid
    private Weights weights = new Weights();

    @AssertTrue(message = "adaptive.recommendation.weights must sum to 1.0")
    public boolean isWeightSumValid() {
        double sum = weights.dueUrgency + weights.weakness + weights.roadmap
                + weights.goal + weights.freshness;
        return Math.abs(sum - 1.0) < 0.0001;
    }

    @Getter
    @Setter
    public static class Weights {
        private double dueUrgency = 0.35;
        private double weakness = 0.25;
        private double roadmap = 0.20;
        private double goal = 0.10;
        private double freshness = 0.10;
    }
}
