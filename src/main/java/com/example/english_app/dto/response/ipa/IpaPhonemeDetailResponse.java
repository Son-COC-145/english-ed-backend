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
            List<IpaExampleWordResponse> exampleWords
    ) {
        this(id, symbol, phonemeType, nameVi, audioMaleUrl, audioFemaleUrl, videoMouthUrl,
                isCommonVnError, pronunciationTipVi, cefrIntroLevel, exampleWords, null);
    }

    public IpaPhonemeDetailResponse withBookmark(Boolean bookmarked) {
        return new IpaPhonemeDetailResponse(id, symbol, phonemeType, nameVi,
                audioMaleUrl, audioFemaleUrl, videoMouthUrl, isCommonVnError,
                pronunciationTipVi, cefrIntroLevel, exampleWords, bookmarked);
    }
}
