package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.LearningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSubmitResponse {
    private Long vocabularyId;
    private LearningStatus previousStatus;
    private LearningStatus newStatus;
    private LocalDateTime nextReviewAt;
    private Integer intervalDays;
    private Short xpEarned;
    private Integer totalXp;
    private Short currentStreak;
}
