package com.example.english_app.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionEvaluationResponse {
    private Short fluencyScore;
    private Short taskCompletionScore;
    private Short intonationScore;
    private String evaluationJson;
}
