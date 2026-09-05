package com.example.english_app.entity.ipa;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ipa_example_words")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpaExampleWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phoneme_id", nullable = false)
    private IpaPhoneme phoneme;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(name = "ipa_transcription", nullable = false, length = 200)
    private String ipaTranscription;

    @Column(name = "audio_url", nullable = false, length = 500)
    private String audioUrl;

    /** Nghĩa tiếng Việt của từ, dùng để hiển thị trong Module 1. */
    @Column(name = "meaning_vi", length = 200)
    private String meaningVi;

    /** URL ảnh minh họa nghĩa của từ (Unsplash hoặc Azure Blob). */
    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
