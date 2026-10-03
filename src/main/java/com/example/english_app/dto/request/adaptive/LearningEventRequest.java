package com.example.english_app.dto.request.adaptive;

import com.example.english_app.entity.enums.DurationSource;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningEventSource;
import com.example.english_app.entity.enums.LearningEventType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

@Value
@Builder
public class LearningEventRequest {
    @NotNull
    @Positive
    Long studentId;

    @NotNull
    LearningEventType eventType;

    @NotNull
    LearningEventSource source;

    @NotBlank
    @Size(max = 120)
    String sourceReference;

    LearnerSkill skill;

    @Size(max = 30)
    String entityType;
    Long entityId;

    @Min(0)
    @Max(100)
    Short score;

    @PositiveOrZero
    Integer durationSeconds;
    DurationSource durationSource;
    JsonNode payload;

    @Positive
    Short schemaVersion;
    LocalDateTime occurredAt;
    UUID causationEventId;
}
