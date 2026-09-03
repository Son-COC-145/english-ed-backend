package com.example.english_app.dto.response.ipa;

import java.io.Serializable;

/**
 * Kết quả đánh giá của một âm vị (phoneme) trong từ được luyện tập.
 * Hỗ trợ các trường mới theo đề xuất Section 4.7 & 4.8 FE report:
 * expectedPhoneme, recognizedPhoneme, errorType, correctionHint, scoreLevel.
 */
public record PhonemeScoreDto(
        String phoneme,
        String expectedPhoneme,
        String recognizedPhoneme,
        Short  accuracyScore,
        String errorType,
        String correctionHint,
        String scoreLevel,
        String color
) implements Serializable {
    private static final long serialVersionUID = 1L;

    public PhonemeScoreDto(String phoneme, Short accuracyScore, String color) {
        this(
            phoneme,
            phoneme,
            phoneme,
            accuracyScore,
            accuracyScore != null && accuracyScore < 60 ? "SUBSTITUTION" : "NONE",
            accuracyScore != null && accuracyScore < 60 ? "Chú ý phát âm rõ và đúng khẩu hình của âm này" : null,
            accuracyScore != null && accuracyScore >= 80 ? "EXCELLENT" : (accuracyScore != null && accuracyScore >= 60 ? "GOOD" : "NEEDS_PRACTICE"),
            color
        );
    }
}
