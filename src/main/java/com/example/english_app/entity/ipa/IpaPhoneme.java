package com.example.english_app.entity.ipa;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ipa_phonemes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpaPhoneme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(nullable = false, unique = true, length = 10)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "phoneme_type", nullable = false)
    private PhonemeType phonemeType;

    @Column(name = "name_vi", nullable = false, length = 100)
    private String nameVi;

    @Column(name = "audio_male_url", nullable = false, length = 500)
    private String audioMaleUrl;

    @Column(name = "audio_female_url", nullable = false, length = 500)
    private String audioFemaleUrl;

    @Column(name = "video_mouth_url", length = 500)
    private String videoMouthUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "cefr_intro_level", nullable = false)
    private CefrLevel cefrIntroLevel;

    @Column(name = "is_common_vn_error", nullable = false)
    @Builder.Default
    private Boolean isCommonVnError = false;

    @OneToMany(mappedBy = "phoneme", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private java.util.List<IpaExampleWord> exampleWords = new java.util.ArrayList<>();
}
