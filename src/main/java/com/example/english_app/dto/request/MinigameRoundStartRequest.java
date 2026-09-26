package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.GameType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MinigameRoundStartRequest {
    @NotNull(message = "Topic ID is required")
    private Short topicId;
    @NotNull(message = "Game Type is required")
    private GameType gameType;
}
