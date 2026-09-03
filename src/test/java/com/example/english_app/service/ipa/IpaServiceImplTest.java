package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.ipa.IpaExampleWord;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.entity.ipa.StudentPhonemeBookmarkId;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.ipa.StudentPhonemeBookmarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho IpaServiceImpl — Phase 1: IPA Sound Library.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("IpaServiceImpl Unit Tests")
class IpaServiceImplTest {

    @Mock private IpaPhonemeRepository             phonemeRepository;
    @Mock private StudentPhonemeBookmarkRepository bookmarkRepository;
    @Mock private UserRepository                   userRepository;

    @InjectMocks
    private IpaServiceImpl ipaService;

    private IpaPhoneme mockPhoneme;

    @BeforeEach
    void setUp() {
        mockPhoneme = IpaPhoneme.builder()
                .id((short) 1)
                .symbol("ʃ")
                .phonemeType(PhonemeType.CONSONANT)
                .nameVi("âm sh")
                .audioMaleUrl("https://cdn/male/sh.mp3")
                .audioFemaleUrl("https://cdn/female/sh.mp3")
                .cefrIntroLevel(CefrLevel.A1)
                .isCommonVnError(true)
                .exampleWords(List.of(
                        IpaExampleWord.builder().id(1L).word("she").build()
                ))
                .build();
    }

    @Test
    @DisplayName("getAllPhonemes: không filter → gọi findByFilters(null, null)")
    void getAllPhonemes_noFilter_callsFindByFilters() {
        given(phonemeRepository.findByFilters(null, null)).willReturn(List.of(mockPhoneme));

        List<IpaPhonemeResponse> result = ipaService.getAllPhonemes(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).symbol()).isEqualTo("ʃ");
        verify(phonemeRepository).findByFilters(null, null);
    }

    @Test
    @DisplayName("getAllPhonemes: filter type=CONSONANT → gọi findByFilters(CONSONANT, null)")
    void getAllPhonemes_withTypeFilter_callsFindByFiltersWithType() {
        given(phonemeRepository.findByFilters(PhonemeType.CONSONANT, null)).willReturn(List.of(mockPhoneme));

        List<IpaPhonemeResponse> result = ipaService.getAllPhonemes(PhonemeType.CONSONANT, null);

        assertThat(result).hasSize(1);
        verify(phonemeRepository).findByFilters(PhonemeType.CONSONANT, null);
    }

    @Test
    @DisplayName("getAllPhonemes: isCommonError=true → chỉ trả về âm lỗi phổ biến")
    void getAllPhonemes_withCommonErrorFilter_returnsOnlyErrors() {
        given(phonemeRepository.findByFilters(null, true)).willReturn(List.of(mockPhoneme));

        List<IpaPhonemeResponse> result = ipaService.getAllPhonemes(null, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isCommonVnError()).isTrue();
    }

    @Test
    @DisplayName("getPhonemeDetail: phoneme tồn tại → trả về detail đầy đủ")
    void getPhonemeDetail_exists_returnsFullDetail() {
        given(phonemeRepository.findByIdWithWords((short) 1)).willReturn(Optional.of(mockPhoneme));

        IpaPhonemeDetailResponse detail = ipaService.getPhonemeDetail((short) 1);

        assertThat(detail.symbol()).isEqualTo("ʃ");
        assertThat(detail.exampleWords()).hasSize(1);
    }

    @Test
    @DisplayName("getPhonemeDetail: không tồn tại → throw PHONEME_NOT_FOUND (404)")
    void getPhonemeDetail_notFound_throwsAppException() {
        given(phonemeRepository.findByIdWithWords((short) 99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ipaService.getPhonemeDetail((short) 99))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(ErrorCode.PHONEME_NOT_FOUND));
    }

    @Test
    @DisplayName("toggleBookmark: chưa có → thêm mới, isBookmarked=true")
    void toggleBookmark_notExists_addsAndReturnsTrue() {
        given(bookmarkRepository.existsById(any(StudentPhonemeBookmarkId.class))).willReturn(false);
        given(userRepository.getReferenceById(1L)).willReturn(null);
        given(phonemeRepository.getReferenceById((short) 1)).willReturn(mockPhoneme);

        com.example.english_app.dto.response.ipa.IpaBookmarkResponse result = ipaService.toggleBookmark(1L, (short) 1);

        assertThat(result.isBookmarked()).isTrue();
        assertThat(result.getPhonemeId()).isEqualTo((short) 1);
        verify(bookmarkRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("toggleBookmark: đã có → xóa, isBookmarked=false")
    void toggleBookmark_exists_removesAndReturnsFalse() {
        given(bookmarkRepository.existsById(any(StudentPhonemeBookmarkId.class))).willReturn(true);

        com.example.english_app.dto.response.ipa.IpaBookmarkResponse result = ipaService.toggleBookmark(1L, (short) 1);

        assertThat(result.isBookmarked()).isFalse();
        assertThat(result.getPhonemeId()).isEqualTo((short) 1);
        verify(bookmarkRepository).deleteById(any(StudentPhonemeBookmarkId.class));
    }
}
