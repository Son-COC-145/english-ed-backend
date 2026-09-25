package com.example.english_app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlacementAnswerRequest {

    /** Stable for one logical submit; the client must reuse it for every network retry. */
    @NotNull(message = "Submission ID không được để trống")
    private UUID submissionId;

    @NotNull(message = "Session ID không được để trống")
    @Positive(message = "Session ID không hợp lệ")
    private Long sessionId;

    @NotNull(message = "Question ID không được để trống")
    @Positive(message = "Question ID không hợp lệ")
    private Long questionId;

    @NotBlank(message = "Câu trả lời không được để trống")
    private String answerGiven;

    @PositiveOrZero(message = "Thời gian trả lời không được âm")
    private Integer timeSpentMs;
}
