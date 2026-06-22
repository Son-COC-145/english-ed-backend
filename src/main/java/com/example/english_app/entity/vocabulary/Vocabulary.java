package com.example.english_app.entity.vocabulary;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.VocabularyStatus;
import com.example.english_app.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "vocabulary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vocabulary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(nullable = false, length = 150)
    private String word;

    @Column(name = "ipa_transcription", nullable = false, length = 300)
    private String ipaTranscription;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_level", nullable = false)
    private CefrLevel cefrLevel;

    @Column(name = "definition_vi", nullable = false, columnDefinition = "TEXT")
    private String definitionVi;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "audio_us_url", nullable = false, length = 500)
    private String audioUsUrl;

    @Column(name = "audio_uk_url", length = 500)
    private String audioUkUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "collocation_json", columnDefinition = "jsonb")
    private String collocationJson;

    @Column(name = "nuance_note", columnDefinition = "TEXT")
    private String nuanceNote;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "example_sentences_json", columnDefinition = "jsonb", nullable = false)
    private String exampleSentencesJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dialogue_json", columnDefinition = "jsonb")
    private String dialogueJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private VocabularyStatus status = VocabularyStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
