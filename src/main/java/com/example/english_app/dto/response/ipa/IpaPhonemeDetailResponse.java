package com.example.english_app.dto.response.ipa;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;

import java.io.Serializable;
import java.util.List;

public record IpaPhonemeDetailResponse(
        Short id,
        String symbol,
        PhonemeType phonemeType,
        String nameVi,
        String audioMaleUrl,
        String audioFemaleUrl,
        /** TTS fallback — luôn có giá trị. FE dùng: {@code audioMaleUrl ?? audioMaleTtsUrl} */
        String audioMaleTtsUrl,
        /** TTS fallback — luôn có giá trị. FE dùng: {@code audioFemaleUrl ?? audioFemaleTtsUrl} */
        String audioFemaleTtsUrl,
        String videoMouthUrl,
        Boolean isCommonVnError,
        String pronunciationTipVi,
        CefrLevel cefrIntroLevel,
        List<IpaExampleWordResponse> exampleWords,
        Boolean isBookmarked
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public IpaPhonemeDetailResponse(
            Short id,
            String symbol,
            PhonemeType phonemeType,
            String nameVi,
            String audioMaleUrl,
            String audioFemaleUrl,
            String videoMouthUrl,
            Boolean isCommonVnError,
            String pronunciationTipVi,
            CefrLevel cefrIntroLevel,
            List<IpaExampleWordResponse> exampleWords,
            Boolean isBookmarked
    ) {
        this(id, symbol, phonemeType, nameVi, audioMaleUrl, audioFemaleUrl,
                buildTtsUrl(symbol, "MALE"), buildTtsUrl(symbol, "FEMALE"),
                videoMouthUrl, isCommonVnError, pronunciationTipVi, cefrIntroLevel, exampleWords, isBookmarked);
    }

    public IpaPhonemeDetailResponse(
            Short id,
            String symbol,
            PhonemeType phonemeType,
            String nameVi,
            String audioMaleUrl,
            String audioFemaleUrl,
            String videoMouthUrl,
            Boolean isCommonVnError,
            String pronunciationTipVi,
            CefrLevel cefrIntroLevel,
            List<IpaExampleWordResponse> exampleWords
    ) {
        this(id, symbol, phonemeType, nameVi, audioMaleUrl, audioFemaleUrl,
                videoMouthUrl, isCommonVnError, pronunciationTipVi, cefrIntroLevel, exampleWords, null);
    }

    private static String buildTtsUrl(String symbolOrWord, String voice) {
        return "/api/v1/ipa/phonemes/tts/stream?text=" +
               java.net.URLEncoder.encode(symbolOrWord != null ? symbolOrWord : "", java.nio.charset.StandardCharsets.UTF_8) +
               "&type=PHONEME&voice=" + voice;
    }

    public IpaPhonemeDetailResponse withBookmark(Boolean bookmarked) {
        return new IpaPhonemeDetailResponse(id, symbol, phonemeType, nameVi,
                audioMaleUrl, audioFemaleUrl, audioMaleTtsUrl, audioFemaleTtsUrl,
                videoMouthUrl, isCommonVnError,
                pronunciationTipVi, cefrIntroLevel, exampleWords, bookmarked);
    }
}
