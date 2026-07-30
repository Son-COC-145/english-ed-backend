package com.example.english_app.dto.response.ipa;

import java.util.List;

/**
 * Response trả về Client sau khi chấm điểm phát âm.
 *
 * @param overallScore        Điểm tổng hợp (weighted: 50% PronScore + 30% Accuracy + 20% Completeness)
 * @param fluencyScore        Điểm trôi chảy
 * @param completenessScore   Điểm đầy đủ (nói đủ các âm)
 * @param stressCorrect       Trọng âm đúng hay không (null nếu từ 1 âm tiết)
 * @param phonemes            Danh sách điểm từng âm vị với color-code
 */
public record PronunciationResultResponse(
        Short               overallScore,
        Short               fluencyScore,
        Short               completenessScore,
        Boolean             stressCorrect,
        List<PhonemeScoreDto> phonemes
) {}
