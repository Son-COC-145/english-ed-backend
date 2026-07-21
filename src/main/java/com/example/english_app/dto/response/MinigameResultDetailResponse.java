package com.example.english_app.dto.response;

import java.time.LocalDateTime;

import com.example.english_app.entity.enums.GameType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MinigameResultDetailResponse {
    private Long id;
    private GameType gameType;
    private Long topicId;
    private String topicName;
    private Short score;
    private Short xpEarned;
    private Short durationSeconds;
    private LocalDateTime playedAt;
}
