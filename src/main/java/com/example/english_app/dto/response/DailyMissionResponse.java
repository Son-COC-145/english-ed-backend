package com.example.english_app.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DailyMissionResponse {
    private List<VocabularyResponse> reviewWords;
    private List<VocabularyResponse> newWords;
}
