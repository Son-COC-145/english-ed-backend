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
@ConfigurationProperties(prefix = "adaptive.worker")
public class AdaptiveWorkerProperties {

    @Min(100)
    private long pollMs = 5_000;

    @Min(1)
    private int batchSize = 50;

    @Min(1)
    private int maxAttempts = 5;

    @Min(1_000)
    private long stuckAfterMs = 600_000;

    @Min(1)
    private int maxBackoffSeconds = 300;
}
