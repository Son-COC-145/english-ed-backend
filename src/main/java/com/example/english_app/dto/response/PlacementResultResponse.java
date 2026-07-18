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
public class PlacementResultResponse {

    private String cefrLevel;
    private int totalQuestions;
    private int correctAnswers;

    private Short vocabScore;
    private Short grammarScore;
    private Short readingScore;
    private Short listeningScore;
    private Short pronunciationScore;

    private String message;
    private String cefrDescription;
    private List<String> strengths;
    private List<String> weaknesses;
    private List<String> suggestedModules;
}
