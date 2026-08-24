package com.example.english_app.service.ipa;

import com.example.english_app.dto.response.ipa.*;
import com.example.english_app.dto.response.ipa.IpaPhonemeBookmarkResponse;
import com.example.english_app.entity.enums.IpaMasteryStatus;
import com.example.english_app.entity.enums.PhonemeType;
import com.example.english_app.entity.enums.PronunciationRuleCategory;
import com.example.english_app.entity.ipa.IpaMinimalPair;
import com.example.english_app.entity.ipa.IpaPhoneme;
import com.example.english_app.entity.ipa.IpaPronunciationRule;
import com.example.english_app.entity.ipa.PronunciationPracticeLog;
import com.example.english_app.entity.ipa.StudentPhonemeBookmark;
import com.example.english_app.entity.ipa.StudentPhonemeBookmarkId;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.ipa.IpaMinimalPairRepository;
import com.example.english_app.repository.ipa.IpaPhonemeRepository;
import com.example.english_app.repository.ipa.IpaPronunciationRuleRepository;
import com.example.english_app.repository.ipa.PhonemeStatProjection;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.ipa.StudentPhonemeBookmarkRepository;
import com.example.english_app.repository.user.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpaServiceImpl implements IpaService {

    private final IpaPhonemeRepository ipaPhonemeRepository;
    private final StudentPhonemeBookmarkRepository bookmarkRepository;
    private final UserRepository userRepository;
    private final PronunciationPracticeLogRepository practiceLogRepository;
    private final IpaMinimalPairRepository minimalPairRepository;
    private final IpaPronunciationRuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Cacheable(
            value = "ipa_phonemes_v2",
            key = "(#type == null ? 'ALL' : #type.name()) + '-' + (#isCommonError == null ? 'ALL' : #isCommonError)",
            sync = true
    )
    @Transactional(readOnly = true)
    public List<IpaPhonemeResponse> getAllPhonemes(PhonemeType type, Boolean isCommonError) {
        return ipaPhonemeRepository.findByFilters(type, isCommonError).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Cacheable(
            value = "ipa_phoneme_detail_v2",
            key = "#id",
            sync = true
    )
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

    @Override
    @Transactional(readOnly = true)
    public IpaMasterySummaryResponse getPhonemeMastery(Long userId) {
        List<IpaPhoneme> allPhonemes = ipaPhonemeRepository.findAll();
        List<PhonemeStatProjection> stats = practiceLogRepository.findPhonemeStatsByStudentId(userId);

        Map<Short, PhonemeStatProjection> statMap = stats.stream()
                .collect(Collectors.toMap(PhonemeStatProjection::getPhonemeId, s -> s));

        int mastered = 0;
        int learning = 0;
        int needsPractice = 0;
        int unlearned = 0;

        List<IpaPhonemeMasteryDto> list = new ArrayList<>();

        for (IpaPhoneme p : allPhonemes) {
            PhonemeStatProjection stat = statMap.get(p.getId());
            IpaMasteryStatus status;
            Short avgScore = null;
            Short maxScore = null;
            Long count = 0L;

            if (stat != null && stat.getAvgScore() != null) {
                avgScore = (short) Math.round(stat.getAvgScore());
                maxScore = stat.getMaxScore();
                count = stat.getPracticeCount();

                if (avgScore >= 80) {
                    status = IpaMasteryStatus.MASTERED;
                    mastered++;
                } else if (avgScore >= 60) {
                    status = IpaMasteryStatus.LEARNING;
                    learning++;
                } else {
                    status = IpaMasteryStatus.NEEDS_PRACTICE;
                    needsPractice++;
                }
            } else {
                status = IpaMasteryStatus.UNLEARNED;
                unlearned++;
            }

            list.add(IpaPhonemeMasteryDto.builder()
                    .id(p.getId())
                    .symbol(p.getSymbol())
                    .phonemeType(p.getPhonemeType())
                    .nameVi(p.getNameVi())
                    .masteryStatus(status)
                    .averageScore(avgScore)
                    .maxScore(maxScore)
                    .practiceCount(count)
                    .isCommonVnError(p.getIsCommonVnError())
                    .build());
        }

        int total = allPhonemes.size();
        double percent = total > 0 ? ((double) mastered / total) * 100.0 : 0.0;

        return IpaMasterySummaryResponse.builder()
                .totalPhonemes(total)
                .masteredCount(mastered)
                .learningCount(learning)
                .needsPracticeCount(needsPractice)
                .unlearnedCount(unlearned)
                .overallMasteryPercent(Math.round(percent * 10.0) / 10.0)
                .phonemes(list)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IpaPracticeHistoryResponse> getPracticeHistory(Long userId, Short phonemeId) {
        List<Object[]> rawList = practiceLogRepository.findHistoryByStudentAndPhonemeRaw(
                userId, phonemeId, PageRequest.of(0, 10));

        return rawList.stream().map(row -> {
            PronunciationPracticeLog logEntry = (PronunciationPracticeLog) row[0];
            String word = (String) row[1];
            String ipa = (String) row[2];

            String color = logEntry.getOverallScore() >= 80 ? "GREEN" :
                    (logEntry.getOverallScore() >= 60 ? "YELLOW" : "RED");

            return IpaPracticeHistoryResponse.builder()
                    .id(logEntry.getId())
                    .word(word)
                    .ipaTranscription(ipa)
                    .overallScore(logEntry.getOverallScore())
                    .fluencyScore(logEntry.getFluencyScore())
                    .completenessScore(logEntry.getCompletenessScore())
                    .stressCorrect(logEntry.getStressCorrect())
                    .scoreColor(color)
                    .practicedAt(logEntry.getPracticedAt())
                    .build();
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<IpaMinimalPairResponse> getMinimalPairs() {
        return minimalPairRepository.findAllActiveWithPhonemes().stream()
                .map(this::mapToMinimalPairResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PronunciationRuleResponse> getPronunciationRules(PronunciationRuleCategory category) {
        return ruleRepository.findByCategory(category).stream()
                .map(r -> PronunciationRuleResponse.builder()
                        .id(r.getId())
                        .category(r.getCategory())
                        .titleVi(r.getTitleVi())
                        .summaryVi(r.getSummaryVi())
                        .orderIndex(r.getOrderIndex())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PronunciationRuleDetailResponse getPronunciationRuleDetail(Long id) {
        IpaPronunciationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRONUNCIATION_RULE_NOT_FOUND));

        List<PronunciationRuleExampleDto> examples = Collections.emptyList();
        try {
            if (rule.getExamplesJson() != null && !rule.getExamplesJson().isBlank()) {
                examples = objectMapper.readValue(
                        rule.getExamplesJson(),
                        new TypeReference<List<PronunciationRuleExampleDto>>() {}
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse examples_json for rule id {}: {}", id, e.getMessage());
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

    private IpaPhonemeResponse mapToResponse(IpaPhoneme p) {
        return new IpaPhonemeResponse(
                p.getId(), p.getSymbol(), p.getPhonemeType(), p.getNameVi(),
                p.getAudioMaleUrl(), p.getAudioFemaleUrl(), p.getIsCommonVnError(), p.getCefrIntroLevel()
        );
    }

    private IpaPhonemeDetailResponse mapToDetailResponse(IpaPhoneme p) {
        var words = p.getExampleWords().stream()
                .map(w -> IpaExampleWordResponse.of(w.getId(), w.getWord(), w.getIpaTranscription(), w.getAudioUrl()))
                .toList();
        return new IpaPhonemeDetailResponse(
                p.getId(), p.getSymbol(), p.getPhonemeType(), p.getNameVi(),
                p.getAudioMaleUrl(), p.getAudioFemaleUrl(), p.getVideoMouthUrl(),
                p.getIsCommonVnError(), p.getPronunciationTipVi(), p.getCefrIntroLevel(), words
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<IpaPhonemeBookmarkResponse> getBookmarkedPhonemes(Long userId) {
        return bookmarkRepository.findByStudentId(userId).stream()
                .map(b -> {
                    IpaPhoneme p = b.getPhoneme();
                    return IpaPhonemeBookmarkResponse.builder()
                            .phonemeId(p.getId())
                            .symbol(p.getSymbol())
                            .phonemeType(p.getPhonemeType())
                            .nameVi(p.getNameVi())
                            .audioMaleUrl(p.getAudioMaleUrl())
                            .audioFemaleUrl(p.getAudioFemaleUrl())
                            .isCommonVnError(p.getIsCommonVnError())
                            .cefrIntroLevel(p.getCefrIntroLevel())
                            .pronunciationTipVi(p.getPronunciationTipVi())
                            .bookmarkedAt(b.getBookmarkedAt())
                            .build();
                })
                .toList();
    }
}

