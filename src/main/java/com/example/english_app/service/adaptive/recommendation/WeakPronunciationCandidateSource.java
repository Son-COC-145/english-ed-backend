package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.config.AdaptiveRecommendationProperties;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemType;
import com.example.english_app.repository.ipa.PhonemeStatProjection;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WeakPronunciationCandidateSource implements TodayPlanCandidateSource {

    private final PronunciationPracticeLogRepository practiceRepository;
    private final AdaptiveRecommendationProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public List<TodayPlanCandidate> collect(TodayPlanContext context) {
        return practiceRepository.findPhonemeStatsByStudentId(context.studentId()).stream()
                .filter(stat -> stat.getAvgScore() != null
                        && stat.getAvgScore() < properties.getWeakPhonemeThreshold())
                .sorted(Comparator.comparingDouble(PhonemeStatProjection::getAvgScore)
                        .thenComparing(PhonemeStatProjection::getPhonemeId))
                .limit(2)
                .map(stat -> candidate(context, stat))
                .toList();
    }

    private TodayPlanCandidate candidate(TodayPlanContext context, PhonemeStatProjection stat) {
        return new TodayPlanCandidate(
                TodayPlanItemType.PRONUNCIATION,
                null,
                LearnerSkill.PRONUNCIATION,
                objectMapper.createObjectNode().put("phonemeId", stat.getPhonemeId()),
                objectMapper.createObjectNode().put("route", "IPA_PHONEME")
                        .put("phonemeId", stat.getPhonemeId()),
                RecommendationReasonCode.WEAK_PHONEME,
                objectMapper.createObjectNode()
                        .put("phonemeId", stat.getPhonemeId())
                        .put("avgScore", Math.round(stat.getAvgScore())),
                null,
                3,
                0,
                1,
                objectMapper.createObjectNode()
                        .put("phonemeId", stat.getPhonemeId())
                        .put("baselinePracticeCount", stat.getPracticeCount()),
                0,
                Math.max(0, 1 - stat.getAvgScore() / 100.0),
                0,
                CandidateSupport.goalFocus(context, LearnerSkill.PRONUNCIATION) ? 1 : 0,
                1,
                null);
    }
}
