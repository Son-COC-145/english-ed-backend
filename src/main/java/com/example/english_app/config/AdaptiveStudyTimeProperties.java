package com.example.english_app.config;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "adaptive.study-time")
public class AdaptiveStudyTimeProperties {

    @Min(1)
    private int maxSecondsPerActivity = 900;

    @Min(1)
    private int vocabularyReviewedSeconds = 10;

    @Min(1)
    private int vocabularyRoundSeconds = 180;

    @Min(1)
    private int pronunciationSeconds = 60;

    @Min(1)
    private int speakingSeconds = 480;
}
