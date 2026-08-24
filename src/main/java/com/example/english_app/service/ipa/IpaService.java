package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.ipa.IpaMasterySummaryResponse;
import com.example.english_app.dto.response.ipa.IpaMinimalPairResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeBookmarkResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeResponse;
import com.example.english_app.dto.response.ipa.IpaPracticeHistoryResponse;
import com.example.english_app.dto.response.ipa.PronunciationRuleDetailResponse;
import com.example.english_app.dto.response.ipa.PronunciationRuleResponse;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.enums.PronunciationRuleCategory;

import java.util.List;
import java.util.Map;

public interface IpaService {
    List<IpaPhonemeResponse> getAllPhonemes(PhonemeType type, Boolean isCommonError);
    IpaPhonemeDetailResponse getPhonemeDetail(Short id);
    Map<String, Boolean> toggleBookmark(Long userId, Short phonemeId);
    IpaMasterySummaryResponse getPhonemeMastery(Long userId);
    List<IpaPracticeHistoryResponse> getPracticeHistory(Long userId, Short phonemeId);
    List<IpaMinimalPairResponse> getMinimalPairs();
    List<PronunciationRuleResponse> getPronunciationRules(PronunciationRuleCategory category);
    PronunciationRuleDetailResponse getPronunciationRuleDetail(Long id);

    /** Lấy danh sách âm học viên đã bookmark (sắp xếp mới nhất trước). */
    List<IpaPhonemeBookmarkResponse> getBookmarkedPhonemes(Long userId);
}
