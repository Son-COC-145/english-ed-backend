package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.LearningStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigameSubmitResponse {
    private Short xpEarned;
    private Integer totalXp;
    private Short currentStreak;
    private LearningStatus newVocabularyStatus;
    private Boolean levelUp;
    private Long resultId;
    /** Round the answer was attached to; its id (once completed) is the resultRefId for VOCABULARY assignments. */
    private Long roundId;
}
