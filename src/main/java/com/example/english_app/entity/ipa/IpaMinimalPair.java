package com.example.english_app.entity.ipa;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ipa_minimal_pairs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpaMinimalPair {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phoneme1_id", nullable = false)
    private IpaPhoneme phoneme1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phoneme2_id", nullable = false)
    private IpaPhoneme phoneme2;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, length = 100)
    private String word1;

    @Column(nullable = false, length = 100)
    private String ipa1;

    @Column(name = "audio1_url", length = 500)
    private String audio1Url;

    @Column(nullable = false, length = 100)
    private String word2;

    @Column(nullable = false, length = 100)
    private String ipa2;

    @Column(name = "audio2_url", length = 500)
    private String audio2Url;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
