package com.example.english_app.service.audio;

import com.example.english_app.dto.response.PronunciationScoreResult;

/**
 * Port (abstraction) cho dịch vụ đánh giá phát âm.
 *
 * <p>Thiết kế theo Dependency Inversion Principle: cả Module 0 (Placement Test)
 * và Module 1 (Speaking Practice) đều phụ thuộc vào interface này.
 * Implementation cụ thể (Azure, Google, v.v.) được Spring inject qua @Primary.
 *
 * <p>Phương thức nhận {@code byte[]} thay vì {@code MultipartFile} để giữ
 * service layer hoàn toàn độc lập với HTTP layer — dễ test và tái sử dụng.
 */
public interface AudioAssessmentPort {

    /**
     * Đánh giá phát âm của một đoạn audio so với từ/câu tham chiếu.
     *
     * <p>Contract:
     * <ul>
     *   <li>Luôn trả về kết quả, không bao giờ throw exception ra ngoài.
     *   <li>Nếu dịch vụ ngoài không khả dụng hoặc timeout → trả về
     *       {@code status = "UNAVAILABLE"} với tất cả score fields là null.
     * </ul>
     *
     * @param audioBytes    Dữ liệu audio thô (WAV, WebM, OGG). Không được null.
     * @param referenceText Từ hoặc câu tham chiếu để so sánh.
     * @return Kết quả đánh giá, không bao giờ null.
     */
    PronunciationScoreResult assess(byte[] audioBytes, String referenceText);
}
