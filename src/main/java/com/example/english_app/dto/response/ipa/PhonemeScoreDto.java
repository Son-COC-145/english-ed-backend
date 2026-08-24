package com.example.english_app.dto.response.ipa;

/**
 * Kết quả đánh giá của một âm vị (phoneme) trong từ được luyện tập.
 * Backend tính toán color-code sẵn để Frontend chỉ cần render màu.
 *
 * @param phoneme       Ký hiệu IPA (vd: "ʃ", "iː")
 * @param accuracyScore Điểm chính xác (0-100). Null nếu Azure unavailable.
 * @param color         "GREEN" (≥80) | "YELLOW" (60-79) | "RED" (<60) | "NONE" (unavailable)
 */
public record PhonemeScoreDto(
        String phoneme,
        Short  accuracyScore,
        String color
) {}
