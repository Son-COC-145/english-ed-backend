package com.example.english_app.service.ipa;

import com.example.english_app.dto.request.ipa.AdminExampleWordRequest;
import com.example.english_app.dto.request.ipa.AdminMinimalPairRequest;
import com.example.english_app.dto.request.ipa.AdminPhonemeUpdateRequest;
import com.example.english_app.dto.request.ipa.AdminPronunciationRuleRequest;
import com.example.english_app.dto.response.ipa.IpaExampleWordResponse;
import com.example.english_app.dto.response.ipa.IpaMinimalPairResponse;
import com.example.english_app.dto.response.ipa.IpaPhonemeDetailResponse;
import com.example.english_app.dto.response.ipa.PronunciationRuleDetailResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.enums.PronunciationRuleCategory;
import com.example.english_app.entity.ipa.IpaExampleWord;
import com.example.english_app.entity.ipa.IpaMinimalPair;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.entity.ipa.IpaPronunciationRule;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.IpaMinimalPairRepository;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.ipa.IpaPronunciationRuleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Admin IPA Management Service Tests")
class AdminIpaServiceTest {

    @Mock private IpaPhonemeRepository ipaPhonemeRepository;
    @Mock private IpaExampleWordRepository exampleWordRepository;
    @Mock private IpaMinimalPairRepository minimalPairRepository;
    @Mock private IpaPronunciationRuleRepository ruleRepository;
    @Spy  private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private AdminIpaServiceImpl adminIpaService;

    @Test
    @DisplayName("Admin cập nhật thông tin âm IPA")
    void testUpdatePhoneme() {
        IpaPhoneme phoneme = IpaPhoneme.builder()
                .id((short) 1)
                .symbol("iː")
                .phonemeType(PhonemeType.VOWEL_MONO)
                .nameVi("Âm i dài cũ")
                .audioMaleUrl("http://audio/male.mp3")
                .audioFemaleUrl("http://audio/female.mp3")
                .cefrIntroLevel(CefrLevel.A1)
                .isCommonVnError(false)
                .build();

        given(ipaPhonemeRepository.findById((short) 1)).willReturn(Optional.of(phoneme));
        given(ipaPhonemeRepository.save(any(IpaPhoneme.class))).willReturn(phoneme);

        AdminPhonemeUpdateRequest request = AdminPhonemeUpdateRequest.builder()
                .nameVi("Âm i dài mới")
                .cefrIntroLevel(CefrLevel.A1)
                .pronunciationTipVi("Cười nhẹ kéo dài mép môi")
                .isCommonVnError(true)
                .build();

        IpaPhonemeDetailResponse response = adminIpaService.updatePhoneme((short) 1, request);

        assertThat(response.nameVi()).isEqualTo("Âm i dài mới");
        assertThat(response.pronunciationTipVi()).isEqualTo("Cười nhẹ kéo dài mép môi");
    }

    @Test
    @DisplayName("Admin thêm từ ví dụ cho âm IPA")
    void testAddExampleWord() {
        IpaPhoneme phoneme = IpaPhoneme.builder().id((short) 1).symbol("iː").build();
        given(ipaPhonemeRepository.findById((short) 1)).willReturn(Optional.of(phoneme));

        IpaExampleWord savedWord = IpaExampleWord.builder()
                .id(100L)
                .phoneme(phoneme)
                .word("sheep")
                .ipaTranscription("/ʃiːp/")
                .audioUrl("http://audio/sheep.mp3")
                .build();
        given(exampleWordRepository.save(any(IpaExampleWord.class))).willReturn(savedWord);

        AdminExampleWordRequest request = AdminExampleWordRequest.builder()
                .word("sheep")
                .ipaTranscription("/ʃiːp/")
                .audioUrl("http://audio/sheep.mp3")
                .build();

        IpaExampleWordResponse response = adminIpaService.addExampleWord((short) 1, request);

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getWord()).isEqualTo("sheep");
    }

    @Test
    @DisplayName("Admin thêm cặp âm đối lập (Minimal Pair)")
    void testCreateMinimalPair() {
        IpaPhoneme p1 = IpaPhoneme.builder().id((short) 1).symbol("iː").build();
        IpaPhoneme p2 = IpaPhoneme.builder().id((short) 2).symbol("ɪ").build();

        given(ipaPhonemeRepository.findById((short) 1)).willReturn(Optional.of(p1));
        given(ipaPhonemeRepository.findById((short) 2)).willReturn(Optional.of(p2));

        IpaMinimalPair savedPair = IpaMinimalPair.builder()
                .id(50L)
                .phoneme1(p1)
                .phoneme2(p2)
                .title("Phân biệt /iː/ và /ɪ/")
                .word1("sheep")
                .ipa1("/ʃiːp/")
                .word2("ship")
                .ipa2("/ʃɪp/")
                .build();
        given(minimalPairRepository.save(any(IpaMinimalPair.class))).willReturn(savedPair);

        AdminMinimalPairRequest request = AdminMinimalPairRequest.builder()
                .phoneme1Id((short) 1)
                .phoneme2Id((short) 2)
                .title("Phân biệt /iː/ và /ɪ/")
                .word1("sheep")
                .ipa1("/ʃiːp/")
                .word2("ship")
                .ipa2("/ʃɪp/")
                .build();

        IpaMinimalPairResponse response = adminIpaService.createMinimalPair(request);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getWord1()).isEqualTo("sheep");
        assertThat(response.getWord2()).isEqualTo("ship");
    }

    @Test
    @DisplayName("Admin thêm quy tắc ngữ âm & trọng âm")
    void testCreatePronunciationRule() {
        IpaPronunciationRule savedRule = IpaPronunciationRule.builder()
                .id(20L)
                .category(PronunciationRuleCategory.WORD_STRESS)
                .titleVi("Quy tắc trọng âm từ 2 âm tiết")
                .summaryVi("Tóm tắt")
                .contentMarkdown("### Markdown...")
                .examplesJson("[]")
                .build();

        given(ruleRepository.save(any(IpaPronunciationRule.class))).willReturn(savedRule);

        AdminPronunciationRuleRequest request = AdminPronunciationRuleRequest.builder()
                .category(PronunciationRuleCategory.WORD_STRESS)
                .titleVi("Quy tắc trọng âm từ 2 âm tiết")
                .summaryVi("Tóm tắt")
                .contentMarkdown("### Markdown...")
                .build();

        PronunciationRuleDetailResponse response = adminIpaService.createPronunciationRule(request);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getTitleVi()).isEqualTo("Quy tắc trọng âm từ 2 âm tiết");
    }

    @Test
    @DisplayName("Admin xóa từ ví dụ")
    void testDeleteExampleWord() {
        given(exampleWordRepository.existsById(100L)).willReturn(true);

        adminIpaService.deleteExampleWord(100L);

        verify(exampleWordRepository).deleteById(100L);
    }
}
