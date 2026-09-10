package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeakingSessionResponse {

    private Long id;
    private String status;
    private Long greetingTurnId;
    private Long studentId;
    private Short scenarioId;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Short hintUsedCount;
    private Short taskCompletionScore;
    private Short fluencyScore;
    private Short intonationScore;
    private Short xpEarned;
    private Object evaluation;
    private List<SpeakingTurnResponse> turns;
}
