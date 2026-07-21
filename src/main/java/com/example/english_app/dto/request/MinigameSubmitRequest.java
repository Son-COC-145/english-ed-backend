package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.GameType;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MinigameSubmitRequest {
    @NotNull(message = "Vocabulary ID is required")
    private Long vocabularyId;

    @NotNull(message = "Game Type is required")
    private GameType gameType;

    @NotNull(message = "Is Correct is required")
    private Boolean isCorrect;

    @NotNull(message = "Duration Seconds is required")
    private Short durationSeconds;
}
