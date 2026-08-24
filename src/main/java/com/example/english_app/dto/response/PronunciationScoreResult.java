package com.example.english_app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Kết quả đánh giá phát âm cho một từ.
 *
 * <p>status = "SCORED"      → overallScore không null, scoreColor được set.
 * <p>status = "UNAVAILABLE" → tất cả score fields là null, scoreColor = "NONE".
 *   Trường hợp này xảy ra khi Azure timeout hoặc không khả dụng.
 *   Luồng Placement Test vẫn tiếp tục bình thường.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PronunciationScoreResult {

    /** Từ được đánh giá */
    private String word;

    /**
     * Điểm tổng hợp (0-100). Null nếu status = "UNAVAILABLE".
     * Thang: ≥80 → Tốt, 60-79 → Trung bình, <60 → Cần cải thiện.
     */
    private Short overallScore;

    /** Điểm chính xác âm vị. Null nếu status = "UNAVAILABLE". */
    private Short accuracyScore;

    /** Điểm trôi chảy. Null nếu status = "UNAVAILABLE". */
    private Short fluencyScore;

    /** Điểm đầy đủ (nói đủ từ). Null nếu status = "UNAVAILABLE". */
    private Short completenessScore;

    /**
     * Màu chỉ mức độ: GREEN (≥80), YELLOW (60-79), RED (<60), NONE (không đánh giá được).
     */
    private String scoreColor;

    /**
     * Trạng thái đánh giá: "SCORED" hoặc "UNAVAILABLE".
     */
    private String status;
}
