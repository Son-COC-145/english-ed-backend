package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionEvaluationResponse {

    private Long sessionId;
    private String status;
    private Short fluencyScore;
    private Short taskCompletionScore;
    private Short intonationScore;
    private Short xpEarned;
    private Short hintUsedCount;
    private Object evaluation;
    private List<SpeakingTurnResponse> turns;
}
