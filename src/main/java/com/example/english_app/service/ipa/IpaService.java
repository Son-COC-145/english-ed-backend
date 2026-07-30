package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeResponse;
import com.example.english_app.entity.enums.PhonemeType;

import java.util.List;
import java.util.Map;

public interface IpaService {
    List<IpaPhonemeResponse> getAllPhonemes(PhonemeType type, Boolean isCommonError);
    IpaPhonemeDetailResponse getPhonemeDetail(Short id);
    Map<String, Boolean> toggleBookmark(Long userId, Short phonemeId);
}
