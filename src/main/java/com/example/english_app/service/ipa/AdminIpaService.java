package com.example.english_app.service.ipa;

import com.example.english_app.dto.request.ipa.AdminExampleWordRequest;
import com.example.english_app.dto.request.ipa.AdminMinimalPairRequest;
import com.example.english_app.dto.request.ipa.AdminPhonemeUpdateRequest;
import com.example.english_app.dto.request.ipa.AdminPronunciationRuleRequest;
import com.example.english_app.dto.response.ipa.IpaExampleWordResponse;
import com.example.english_app.dto.response.ipa.IpaMinimalPairResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.PronunciationRuleDetailResponse;

public interface AdminIpaService {
    // 1. Quản lý âm IPA
    IpaPhonemeDetailResponse updatePhoneme(Short id, AdminPhonemeUpdateRequest request);

    // 2. Quản lý từ ví dụ cho âm
    IpaExampleWordResponse addExampleWord(Short phonemeId, AdminExampleWordRequest request);
    IpaExampleWordResponse updateExampleWord(Long wordId, AdminExampleWordRequest request);
    void deleteExampleWord(Long wordId);

    // 3. Quản lý cặp âm đối lập (Minimal Pairs)
    IpaMinimalPairResponse createMinimalPair(AdminMinimalPairRequest request);
    IpaMinimalPairResponse updateMinimalPair(Long id, AdminMinimalPairRequest request);
    void deleteMinimalPair(Long id);

    // 4. Quản lý quy tắc ngữ âm & trọng âm (Rules)
    PronunciationRuleDetailResponse createPronunciationRule(AdminPronunciationRuleRequest request);
    PronunciationRuleDetailResponse updatePronunciationRule(Long id, AdminPronunciationRuleRequest request);
    void deletePronunciationRule(Long id);
}
