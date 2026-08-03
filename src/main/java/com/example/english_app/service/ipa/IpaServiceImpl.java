package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.ipa.IpaExampleWordResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeResponse;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.entity.ipa.StudentPhonemeBookmark;
import com.example.english_app.entity.ipa.StudentPhonemeBookmarkId;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.ipa.StudentPhonemeBookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class IpaServiceImpl implements IpaService {

    private final IpaPhonemeRepository ipaPhonemeRepository;
    private final StudentPhonemeBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;

    @Override
    @Cacheable(value = "ipa_phonemes", key = "{#type, #isCommonError}", sync = true)
    @Transactional(readOnly = true)
    public List<IpaPhonemeResponse> getAllPhonemes(PhonemeType type, Boolean isCommonError) {
        return ipaPhonemeRepository.findByFilters(type, isCommonError).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "ipa_phoneme_detail", key = "#id", sync = true)
    @Transactional(readOnly = true)
    public IpaPhonemeDetailResponse getPhonemeDetail(Short id) {
        return ipaPhonemeRepository.findByIdWithWords(id)
                .map(this::mapToDetailResponse)
                .orElseThrow(() -> new AppException(ErrorCode.PHONEME_NOT_FOUND));
    }

    @Override
    @Transactional
    public Map<String, Boolean> toggleBookmark(Long userId, Short phonemeId) {
        var id = new StudentPhonemeBookmarkId(userId, phonemeId);
        if (bookmarkRepository.existsById(id)) {
            bookmarkRepository.deleteById(id);
            return Map.of("isBookmarked", false);
        } else {
            var bookmark = StudentPhonemeBookmark.builder()
                    .id(id)
                    .student(userRepository.getReferenceById(userId))
                    .phoneme(ipaPhonemeRepository.getReferenceById(phonemeId))
                    .build();
            bookmarkRepository.save(bookmark);
            return Map.of("isBookmarked", true);
        }
    }

    private IpaPhonemeResponse mapToResponse(IpaPhoneme p) {
        return new IpaPhonemeResponse(
                p.getId(), p.getSymbol(), p.getPhonemeType(), p.getNameVi(),
                p.getAudioMaleUrl(), p.getAudioFemaleUrl(), p.getIsCommonVnError(), p.getCefrIntroLevel()
        );
    }

    private IpaPhonemeDetailResponse mapToDetailResponse(IpaPhoneme p) {
        var words = p.getExampleWords().stream()
                .map(w -> new IpaExampleWordResponse(w.getId(), w.getWord(), w.getIpaTranscription(), w.getAudioUrl()))
                .toList();
        return new IpaPhonemeDetailResponse(
                p.getId(), p.getSymbol(), p.getPhonemeType(), p.getNameVi(),
                p.getAudioMaleUrl(), p.getAudioFemaleUrl(), p.getVideoMouthUrl(),
                p.getIsCommonVnError(), p.getCefrIntroLevel(), words
        );
    }
}
