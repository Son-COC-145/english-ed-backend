package com.example.english_app.dto.response;

import java.time.LocalDateTime;

import com.example.english_app.entity.enums.LearningStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentVocabularyProgressResponse {
    private Long id;
    private Long vocabularyId;
    private String word;
    private LearningStatus status;
    private LocalDateTime nextReviewAt;
    private Short correctCount;
    private Short incorrectCount;
    private LocalDateTime lastPracticedAt;
}
