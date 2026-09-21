package com.example.english_app.dto.response;

import com.example.english_app.entity.enums.LearningStatus;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

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
    private String nuanceNote;

    // Typed JSON fields
    private List<ExampleSentence> exampleSentences;
    private List<Collocation> collocations;
    private List<DialogueLine> dialogue;

    private String status;
    private UserBrief createdBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    /**
     * Tiến trình học của student hiện tại với từ này.
     * Null nếu student chưa học từ này, hoặc requester không phải student.
     */
    private UserProgressBrief userProgress;

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

    /** Projection nhỏ gọn của progress – chỉ expose những gì Mobile cần để render list */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProgressBrief {
        private LearningStatus status;
        private LocalDateTime lastReviewedAt;
        private LocalDateTime nextReviewAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExampleSentence {
        @JsonProperty("sentence")
        @JsonAlias({"en", "sentence"})
        private String sentence;

        @JsonProperty("translationVi")
        @JsonAlias({"vi", "translationVi"})
        private String translationVi;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Collocation {
        @JsonProperty("phrase")
        @JsonAlias({"collocation", "phrase"})
        private String phrase;

        @JsonProperty("meaningVi")
        @JsonAlias({"vi", "meaningVi"})
        private String meaningVi;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DialogueLine {
        private String speaker;

        @JsonProperty("text")
        @JsonAlias({"en", "text", "sentence"})
        private String text;

        @JsonProperty("translationVi")
        @JsonAlias({"vi", "translationVi"})
        private String translationVi;
    }
}
