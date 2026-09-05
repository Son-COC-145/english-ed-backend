package com.example.english_app.service.ipa;

import com.example.english_app.dto.request.ipa.AdminExampleWordRequest;
import com.example.english_app.dto.request.ipa.AdminMinimalPairRequest;
import com.example.english_app.dto.request.ipa.AdminPhonemeUpdateRequest;
import com.example.english_app.dto.request.ipa.AdminPronunciationRuleRequest;
import com.example.english_app.dto.response.ipa.*;
import com.example.english_app.entity.ipa.IpaExampleWord;
import com.example.english_app.entity.ipa.IpaMinimalPair;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.entity.ipa.IpaPronunciationRule;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.IpaMinimalPairRepository;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.ipa.IpaPronunciationRuleRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminIpaServiceImpl implements AdminIpaService {

    private final IpaPhonemeRepository ipaPhonemeRepository;
    private final IpaExampleWordRepository exampleWordRepository;
    private final IpaMinimalPairRepository minimalPairRepository;
    private final IpaPronunciationRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    // ─── 1. Phoneme Management ───────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {"ipa_phonemes_v2", "ipa_phoneme_detail_v2"}, allEntries = true)
    public IpaPhonemeDetailResponse updatePhoneme(Short id, AdminPhonemeUpdateRequest request) {
        IpaPhoneme phoneme = ipaPhonemeRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PHONEME_NOT_FOUND));

        phoneme.setNameVi(request.getNameVi());
        if (request.getAudioMaleUrl() != null) phoneme.setAudioMaleUrl(request.getAudioMaleUrl());
        if (request.getAudioFemaleUrl() != null) phoneme.setAudioFemaleUrl(request.getAudioFemaleUrl());
        if (request.getVideoMouthUrl() != null) phoneme.setVideoMouthUrl(request.getVideoMouthUrl());
        phoneme.setCefrIntroLevel(request.getCefrIntroLevel());
        if (request.getIsCommonVnError() != null) phoneme.setIsCommonVnError(request.getIsCommonVnError());
        if (request.getPronunciationTipVi() != null) phoneme.setPronunciationTipVi(request.getPronunciationTipVi());

        ipaPhonemeRepository.save(phoneme);

        return mapToDetailResponse(phoneme);
    }

    // ─── 2. Example Word Management ──────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = {"ipa_phonemes_v2", "ipa_phoneme_detail_v2"}, allEntries = true)
    public IpaExampleWordResponse addExampleWord(Short phonemeId, AdminExampleWordRequest request) {
        IpaPhoneme phoneme = ipaPhonemeRepository.findById(phonemeId)
                .orElseThrow(() -> new AppException(ErrorCode.PHONEME_NOT_FOUND));

        IpaExampleWord word = IpaExampleWord.builder()
                .phoneme(phoneme)
                .word(request.getWord())
                .ipaTranscription(request.getIpaTranscription())
                .audioUrl(request.getAudioUrl())
                .meaningVi(request.getMeaningVi())
                .imageUrl(request.getImageUrl())
                .build();

        IpaExampleWord saved = exampleWordRepository.save(word);
        return IpaExampleWordResponse.of(saved.getId(), saved.getWord(), saved.getIpaTranscription(),
                saved.getAudioUrl(), saved.getMeaningVi(), saved.getImageUrl());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"ipa_phonemes_v2", "ipa_phoneme_detail_v2"}, allEntries = true)
    public IpaExampleWordResponse updateExampleWord(Long wordId, AdminExampleWordRequest request) {
        IpaExampleWord word = exampleWordRepository.findById(wordId)
                .orElseThrow(() -> new AppException(ErrorCode.EXAMPLE_WORD_NOT_FOUND));

        word.setWord(request.getWord());
        word.setIpaTranscription(request.getIpaTranscription());
        word.setAudioUrl(request.getAudioUrl());
        word.setMeaningVi(request.getMeaningVi());
        word.setImageUrl(request.getImageUrl());

        IpaExampleWord saved = exampleWordRepository.save(word);
        return IpaExampleWordResponse.of(saved.getId(), saved.getWord(), saved.getIpaTranscription(),
                saved.getAudioUrl(), saved.getMeaningVi(), saved.getImageUrl());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"ipa_phonemes_v2", "ipa_phoneme_detail_v2"}, allEntries = true)
    public void deleteExampleWord(Long wordId) {
        if (!exampleWordRepository.existsById(wordId)) {
            throw new AppException(ErrorCode.EXAMPLE_WORD_NOT_FOUND);
        }
        exampleWordRepository.deleteById(wordId);
    }

    // ─── 3. Minimal Pair Management ──────────────────────────────────────────

    @Override
    @Transactional
    public IpaMinimalPairResponse createMinimalPair(AdminMinimalPairRequest request) {
        IpaPhoneme p1 = ipaPhonemeRepository.findById(request.getPhoneme1Id())
                .orElseThrow(() -> new AppException(ErrorCode.PHONEME_NOT_FOUND));
        IpaPhoneme p2 = ipaPhonemeRepository.findById(request.getPhoneme2Id())
                .orElseThrow(() -> new AppException(ErrorCode.PHONEME_NOT_FOUND));

        IpaMinimalPair pair = IpaMinimalPair.builder()
                .phoneme1(p1)
                .phoneme2(p2)
                .title(request.getTitle())
                .description(request.getDescription())
                .word1(request.getWord1())
                .ipa1(request.getIpa1())
                .audio1Url(request.getAudio1Url())
                .word2(request.getWord2())
                .ipa2(request.getIpa2())
                .audio2Url(request.getAudio2Url())
                .isActive(true)
                .build();

        IpaMinimalPair saved = minimalPairRepository.save(pair);
        return mapToMinimalPairResponse(saved);
    }

    @Override
    @Transactional
    public IpaMinimalPairResponse updateMinimalPair(Long id, AdminMinimalPairRequest request) {
        IpaMinimalPair pair = minimalPairRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MINIMAL_PAIR_NOT_FOUND));

        IpaPhoneme p1 = ipaPhonemeRepository.findById(request.getPhoneme1Id())
                .orElseThrow(() -> new AppException(ErrorCode.PHONEME_NOT_FOUND));
        IpaPhoneme p2 = ipaPhonemeRepository.findById(request.getPhoneme2Id())
                .orElseThrow(() -> new AppException(ErrorCode.PHONEME_NOT_FOUND));

        pair.setPhoneme1(p1);
        pair.setPhoneme2(p2);
        pair.setTitle(request.getTitle());
        pair.setDescription(request.getDescription());
        pair.setWord1(request.getWord1());
        pair.setIpa1(request.getIpa1());
        pair.setAudio1Url(request.getAudio1Url());
        pair.setWord2(request.getWord2());
        pair.setIpa2(request.getIpa2());
        pair.setAudio2Url(request.getAudio2Url());

        IpaMinimalPair saved = minimalPairRepository.save(pair);
        return mapToMinimalPairResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMinimalPair(Long id) {
        if (!minimalPairRepository.existsById(id)) {
            throw new AppException(ErrorCode.MINIMAL_PAIR_NOT_FOUND);
        }
        minimalPairRepository.deleteById(id);
    }

    // ─── 4. Pronunciation Rule Management ────────────────────────────────────

    @Override
    @Transactional
    public PronunciationRuleDetailResponse createPronunciationRule(AdminPronunciationRuleRequest request) {
        IpaPronunciationRule rule = IpaPronunciationRule.builder()
                .category(request.getCategory())
                .titleVi(request.getTitleVi())
                .summaryVi(request.getSummaryVi())
                .contentMarkdown(request.getContentMarkdown())
                .examplesJson(request.getExamplesJson() != null ? request.getExamplesJson() : "[]")
                .orderIndex(request.getOrderIndex() != null ? request.getOrderIndex() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        IpaPronunciationRule saved = ruleRepository.save(rule);
        return mapToRuleDetailResponse(saved);
    }

    @Override
    @Transactional
    public PronunciationRuleDetailResponse updatePronunciationRule(Long id, AdminPronunciationRuleRequest request) {
        IpaPronunciationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRONUNCIATION_RULE_NOT_FOUND));

        rule.setCategory(request.getCategory());
        rule.setTitleVi(request.getTitleVi());
        rule.setSummaryVi(request.getSummaryVi());
        rule.setContentMarkdown(request.getContentMarkdown());
        if (request.getExamplesJson() != null) rule.setExamplesJson(request.getExamplesJson());
        if (request.getOrderIndex() != null) rule.setOrderIndex(request.getOrderIndex());
        if (request.getIsActive() != null) rule.setIsActive(request.getIsActive());

        IpaPronunciationRule saved = ruleRepository.save(rule);
        return mapToRuleDetailResponse(saved);
    }

    @Override
    @Transactional
    public void deletePronunciationRule(Long id) {
        if (!ruleRepository.existsById(id)) {
            throw new AppException(ErrorCode.PRONUNCIATION_RULE_NOT_FOUND);
        }
        ruleRepository.deleteById(id);
    }

    // ─── Helper Mappers ──────────────────────────────────────────────────────

    private IpaPhonemeDetailResponse mapToDetailResponse(IpaPhoneme p) {
        var words = p.getExampleWords() != null ? p.getExampleWords().stream()
                .map(w -> IpaExampleWordResponse.of(w.getId(), w.getWord(), w.getIpaTranscription(),
                        w.getAudioUrl(), w.getMeaningVi(), w.getImageUrl()))
                .toList() : Collections.<IpaExampleWordResponse>emptyList();

        return new IpaPhonemeDetailResponse(
                p.getId(), p.getSymbol(), p.getPhonemeType(), p.getNameVi(),
                p.getAudioMaleUrl(), p.getAudioFemaleUrl(), p.getVideoMouthUrl(),
                p.getIsCommonVnError(), p.getPronunciationTipVi(), p.getCefrIntroLevel(), words
        );
    }

    private IpaMinimalPairResponse mapToMinimalPairResponse(IpaMinimalPair m) {
        return IpaMinimalPairResponse.builder()
                .id(m.getId())
                .title(m.getTitle())
                .description(m.getDescription())
                .phoneme1Id(m.getPhoneme1().getId())
                .phoneme1Symbol(m.getPhoneme1().getSymbol())
                .word1(m.getWord1())
                .ipa1(m.getIpa1())
                .audio1Url(m.getAudio1Url())
                .phoneme2Id(m.getPhoneme2().getId())
                .phoneme2Symbol(m.getPhoneme2().getSymbol())
                .word2(m.getWord2())
                .ipa2(m.getIpa2())
                .audio2Url(m.getAudio2Url())
                .build();
    }

    private PronunciationRuleDetailResponse mapToRuleDetailResponse(IpaPronunciationRule rule) {
        List<PronunciationRuleExampleDto> examples = Collections.emptyList();
        try {
            if (rule.getExamplesJson() != null && !rule.getExamplesJson().isBlank()) {
                examples = objectMapper.readValue(
                        rule.getExamplesJson(),
                        new TypeReference<List<PronunciationRuleExampleDto>>() {}
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse examples_json for rule id {}: {}", rule.getId(), e.getMessage());
        }

        return PronunciationRuleDetailResponse.builder()
                .id(rule.getId())
                .category(rule.getCategory())
                .titleVi(rule.getTitleVi())
                .summaryVi(rule.getSummaryVi())
                .contentMarkdown(rule.getContentMarkdown())
                .examples(examples)
                .orderIndex(rule.getOrderIndex())
                .build();
    }
}
