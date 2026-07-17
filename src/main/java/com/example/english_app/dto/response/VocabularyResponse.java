package com.example.english_app.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VocabularyResponse {

    private Long id;
    private TopicBrief topic;
    private String word;
    private String ipaTranscription;
    private String cefrLevel;
    private String definitionVi;
    private String imageUrl;
    private String audioUsUrl;
    private String audioUkUrl;
    private String collocationJson;
    private String nuanceNote;
    private String exampleSentencesJson;
    private String dialogueJson;
    private String status;
    private UserBrief createdBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopicBrief {
        private Short id;
        private String nameEn;
        private String nameVi;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserBrief {
        private Long id;
        private String fullName;
        private String email;
    }
}
