package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.GameType;
import com.example.english_app.entity.enums.MinigameRoundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinigameRoundResponse {
    /** Use as {@code resultRefId} when submitting a VOCABULARY assignment (round must be COMPLETED). */
    private Long id;
    private Short topicId;
    private String topicName;
    private GameType gameType;
    private MinigameRoundStatus status;
    private Integer totalQuestions;
    private Integer correctCount;
    private Short score;
    private Integer xpEarned;
    private Integer durationSeconds;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
