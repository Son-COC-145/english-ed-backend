package com.example.english_app.dto.ai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EvaluationResultDto {
    @JsonProperty("turns_evaluation")
    private List<TurnEvaluation> turnsEvaluation;

    @JsonProperty("task_completion_score")
    private Short taskCompletionScore;

    @JsonProperty("intonation_score")
    private Short intonationScore;

    @JsonProperty("general_feedback")
    private GeneralFeedback generalFeedback;

    @Data
    public static class TurnEvaluation {
        @JsonProperty("turn_index")
        private Short turnIndex;
        
        @JsonProperty("grammar_errors")
        private List<Map<String, String>> grammarErrors;
        
        @JsonProperty("vocabulary_suggestions")
        private List<Map<String, String>> vocabularySuggestions;
    }

    @Data
    public static class GeneralFeedback {
        private String strengths;
        private String weaknesses;
        @JsonProperty("overall_feedback")
        private String overallFeedback;
    }
}
