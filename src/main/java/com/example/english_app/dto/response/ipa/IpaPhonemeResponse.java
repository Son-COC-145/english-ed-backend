package com.example.english_app.dto.response.ipa;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;

import java.io.Serializable;

public record IpaPhonemeResponse(
        Short id,
        String symbol,
        PhonemeType phonemeType,
        String nameVi,
        String audioMaleUrl,
        String audioFemaleUrl,
        Boolean isCommonVnError,
        CefrLevel cefrIntroLevel
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
