package com.example.english_app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlacementAnswerRequest {

    @NotNull(message = "Session ID không được để trống")
    private Long sessionId;

    @NotNull(message = "Question ID không được để trống")
    private Long questionId;

    private String answerGiven;

    private Integer timeSpentMs;
}
