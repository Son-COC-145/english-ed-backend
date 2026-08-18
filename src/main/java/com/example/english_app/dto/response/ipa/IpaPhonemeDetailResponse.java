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
        List<IpaExampleWordResponse> exampleWords
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
