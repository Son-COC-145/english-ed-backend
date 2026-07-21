package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.LearningStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MinigameSubmitResponse {
    private Short xpEarned;
    private Integer totalXp;
    private Short currentStreak;
    private LearningStatus newVocabularyStatus;
    private Boolean levelUp;
}
