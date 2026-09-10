package com.example.english_app.dto.request;

import com.example.english_app.entity.enums.GameType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    /** UUID do Mobile tạo để chống duplicate submit. Khuyến nghị luôn truyền. */
    @Size(min = 36, max = 36, message = "attemptId phải là UUID 36 ký tự")
    private String attemptId;
}
