package com.example.english_app.dto.response.ipa;

import java.io.Serializable;
import java.util.List;

/**
 * Response trả về Client sau khi chấm điểm phát âm.
 * Bổ sung các ID định danh bền vững (Section 4.10): practiceId, exampleWordId, phonemeId, scoreLevel.
 */
public record PronunciationResultResponse(
        Long                practiceId,
        Long                exampleWordId,
        Short               phonemeId,
        Short               overallScore,
        String              scoreLevel,
        Short               fluencyScore,
        Short               completenessScore,
        Boolean             stressCorrect,
        List<PhonemeScoreDto> phonemes
) implements Serializable {
    private static final long serialVersionUID = 1L;

    // Constructor tương thích ngược cho các class hoặc test chưa truyền ID
    public PronunciationResultResponse(
            Short overallScore,
            Short fluencyScore,
            Short completenessScore,
            Boolean stressCorrect,
            List<PhonemeScoreDto> phonemes
    ) {
        this(null, null, null, overallScore,
                overallScore != null && overallScore >= 80 ? "EXCELLENT" : (overallScore != null && overallScore >= 60 ? "GOOD" : "NEEDS_PRACTICE"),
                fluencyScore, completenessScore, stressCorrect, phonemes);
    }
}
