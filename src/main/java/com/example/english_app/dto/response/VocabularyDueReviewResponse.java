package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.LearningStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Một từ trong hàng đợi ôn tập SRS – đủ data để render Flashcard */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VocabularyDueReviewResponse {
    private Long progressId;
    private LearningStatus status;
    private LocalDateTime nextReviewAt;
    private Long vocabularyId;
    private String word;
    private String ipaTranscription;
    private String definitionVi;
    private String imageUrl;
    private String audioUsUrl;
    private String audioUkUrl;
    private String cefrLevel;
    private Long topicId;
    private String topicNameEn;
}
