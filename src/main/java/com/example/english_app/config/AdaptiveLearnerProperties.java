package com.example.english_app.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "adaptive.learner")
public class AdaptiveLearnerProperties {

    @DecimalMin("0.01")
    @DecimalMax("1.0")
    private double alpha = 0.2;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double placementConfidence = 0.3;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double vocabularyReviewReliability = 0.3;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double pronunciationReliability = 0.6;

    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private double speakingReliability = 1.0;
}
