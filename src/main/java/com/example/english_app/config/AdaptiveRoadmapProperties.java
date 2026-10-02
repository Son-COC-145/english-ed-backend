package com.example.english_app.config;

import jakarta.validation.constraints.Max;
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
@ConfigurationProperties(prefix = "adaptive.roadmap")
public class AdaptiveRoadmapProperties {

    @Min(1)
    @Max(12)
    private int ipaMaxPerModule = 12;
}
