package com.example.english_app.dto.response.ipa;

import java.io.Serializable;

public record IpaExampleWordResponse(
        Long id,
        String word,
        String ipaTranscription,
        String audioUrl
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
