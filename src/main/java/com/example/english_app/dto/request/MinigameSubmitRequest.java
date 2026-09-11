package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.GameType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "attemptId is required")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$", message = "attemptId must be a valid UUID")
    private String attemptId;
}
