package com.example.english_app.service.adaptive.learner;

import com.example.english_app.config.AdaptiveLearnerProperties;
import com.example.english_app.dto.response.adaptive.LearnerProfileResponse;
import com.example.english_app.dto.response.adaptive.LearnerSkillResponse;
import com.example.english_app.entity.adaptive.LearnerProfile;
import com.example.english_app.entity.adaptive.LearnerSkillState;
import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearnerEvidenceSource;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.entity.enums.MasteryTrend;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.adaptive.LearnerProfileRepository;
import com.example.english_app.repository.adaptive.LearnerSkillStateRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LearnerProfileService {

    private static final String CEFR_SOURCE_PLACEMENT = "PLACEMENT";

    private final LearnerProfileRepository profileRepository;
    private final LearnerSkillStateRepository skillRepository;
    private final OnboardingRepository onboardingRepository;
    private final AdaptiveLearnerProperties properties;

    @Transactional
    public LearnerProfile ensureProfile(Long studentId) {
        return profileRepository.findById(studentId).orElseGet(() -> {
            StudentOnboarding onboarding = onboardingRepository.findByStudentId(studentId)
                    .filter(value -> value.getPlacementCefrLevel() != null)
                    .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_INCOMPLETE.toException());
            return bootstrapFromOnboarding(onboarding, false);
        });
    }

    @Transactional
    public LearnerProfileResponse getProfile(Long studentId) {
        LearnerProfile profile = ensureProfile(studentId);
        return toResponse(profile, skillRepository.findByStudentIdOrderBySkillAsc(studentId));
    }

    @Transactional
    public void applyPlacement(LearningEvent event) {
        JsonNode payload = event.getPayload();
        if (payload == null || payload.path("overallCefr").isMissingNode()) {
            throw new IllegalArgumentException("PLACEMENT_COMPLETED payload is missing overallCefr");
        }

        LearnerProfile profile = profileRepository.findById(event.getStudentId()).orElseGet(() -> newProfile(
                event.getStudentId(), event.getOccurredAt()));
        profile.setCefrLevel(CefrLevel.valueOf(payload.path("overallCefr").asText()));
        profile.setCefrSource(CEFR_SOURCE_PLACEMENT);
        profile.setCefrAssessedAt(event.getOccurredAt());
        profile.setProfileVersion(profile.getProfileVersion() + 1);
        profile.setUpdatedAt(LocalDateTime.now());
        profileRepository.save(profile);

        JsonNode skills = payload.path("skills");
        for (LearnerSkill skill : LearnerSkill.values()) {
            if (skill == LearnerSkill.SPEAKING) continue;
            JsonNode node = skills.path(skill.name());
            if (!node.isObject() || !node.path("score").canConvertToInt()) continue;
            applyPlacementSkill(
                    event.getStudentId(),
                    skill,
                    node.path("score").intValue(),
                    parseCefr(node.path("cefr").asText(null)),
                    event.getOccurredAt());
        }
        recalculateOverall(profile);
    }

    @Transactional
    public void applyPractice(LearningEvent event) {
        PracticeObservation observation = observation(event);
        LearnerProfile profile = ensureProfile(event.getStudentId());
        LocalDateTime now = LocalDateTime.now();
        LearnerSkillState state = skillRepository
                .findByStudentIdAndSkill(event.getStudentId(), observation.skill())
                .orElseGet(() -> newSkillState(event.getStudentId(), observation.skill(), now));

        double oldMastery = state.getMastery() == null
                ? observation.score()
                : state.getMastery().doubleValue();
        double mastery = state.getMastery() == null
                ? observation.score()
                : oldMastery + properties.getAlpha() * observation.reliability()
                        * (observation.score() - oldMastery);
        double oldRecent = state.getRecentScore() == null
                ? observation.score()
                : state.getRecentScore().doubleValue();
        double recent = state.getRecentScore() == null
                ? observation.score()
                : oldRecent + 0.5 * (observation.score() - oldRecent);
        double evidenceWeight = state.getEvidenceWeight().doubleValue() + observation.reliability();

        state.setMastery(decimal(mastery, 2));
        state.setRecentScore(decimal(recent, 2));
        state.setEvidenceWeight(scaled(evidenceWeight, 3));
        state.setEvidenceCount(state.getEvidenceCount() + 1);
        state.setConfidence(decimal(1.0 - Math.exp(-evidenceWeight / 5.0), 3));
        state.setTrend(trend(recent, mastery));
        state.setSource(LearnerEvidenceSource.PRACTICE);
        state.setLastPracticedAt(event.getOccurredAt());
        state.setUpdatedAt(now);
        skillRepository.save(state);

        profile.setProfileVersion(profile.getProfileVersion() + 1);
        profile.setTotalActivities(profile.getTotalActivities() + 1);
        profile.setUpdatedAt(now);
        recalculateOverall(profile);
    }

    private LearnerProfile bootstrapFromOnboarding(StudentOnboarding onboarding, boolean incrementVersion) {
        LocalDateTime now = LocalDateTime.now();
        Long studentId = onboarding.getStudent().getId();
        LearnerProfile profile = newProfile(studentId, now);
        profile.setCefrLevel(onboarding.getPlacementCefrLevel());
        profile.setCefrSource(CEFR_SOURCE_PLACEMENT);
        profile.setCefrAssessedAt(onboarding.getPlacementCompletedAt());
        profile.setProfileVersion(incrementVersion ? 1L : 0L);
        profileRepository.save(profile);

        Map<LearnerSkill, PlacementSkill> placement = new EnumMap<>(LearnerSkill.class);
        placement.put(LearnerSkill.VOCABULARY, new PlacementSkill(
                onboarding.getPlacementVocabScore(), onboarding.getPlacementVocabCefr()));
        placement.put(LearnerSkill.GRAMMAR, new PlacementSkill(
                onboarding.getPlacementGrammarScore(), onboarding.getPlacementGrammarCefr()));
        placement.put(LearnerSkill.READING, new PlacementSkill(
                onboarding.getPlacementReadingScore(), onboarding.getPlacementReadingCefr()));
        placement.put(LearnerSkill.LISTENING, new PlacementSkill(
                onboarding.getPlacementListeningScore(), onboarding.getPlacementListeningCefr()));
        placement.put(LearnerSkill.PRONUNCIATION, new PlacementSkill(
                onboarding.getPlacementPronunciationScore(), onboarding.getPlacementPronunciationCefr()));
        placement.forEach((skill, value) -> {
            if (value.score() != null) {
                applyPlacementSkill(studentId, skill, value.score(), value.cefr(),
                        onboarding.getPlacementCompletedAt());
            }
        });
        recalculateOverall(profile);
        return profile;
    }

    private void applyPlacementSkill(
            Long studentId,
            LearnerSkill skill,
            int score,
            CefrLevel cefr,
            LocalDateTime occurredAt) {
        LearnerSkillState state = skillRepository.findByStudentIdAndSkill(studentId, skill)
                .orElseGet(() -> newSkillState(studentId, skill, occurredAt));
        state.setCefrLevel(cefr);
        if (state.getSource() != LearnerEvidenceSource.PRACTICE) {
            state.setSource(LearnerEvidenceSource.PLACEMENT);
            state.setMastery(decimal(score, 2));
            state.setRecentScore(decimal(score, 2));
            state.setConfidence(decimal(properties.getPlacementConfidence(), 3));
            state.setEvidenceWeight(BigDecimal.ZERO.setScale(3));
            state.setEvidenceCount(0);
            state.setTrend(MasteryTrend.STABLE);
        }
        state.setUpdatedAt(LocalDateTime.now());
        skillRepository.save(state);
    }

    private LearnerProfile newProfile(Long studentId, LocalDateTime now) {
        return LearnerProfile.builder()
                .studentId(studentId)
                .profileVersion(0L)
                .totalActivities(0)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private LearnerSkillState newSkillState(Long studentId, LearnerSkill skill, LocalDateTime now) {
        return LearnerSkillState.builder()
                .studentId(studentId)
                .skill(skill)
                .source(LearnerEvidenceSource.PLACEMENT)
                .confidence(BigDecimal.ZERO.setScale(3))
                .evidenceWeight(BigDecimal.ZERO.setScale(3))
                .evidenceCount(0)
                .trend(MasteryTrend.STABLE)
                .updatedAt(now)
                .build();
    }

    private void recalculateOverall(LearnerProfile profile) {
        List<LearnerSkillState> states = skillRepository.findByStudentIdOrderBySkillAsc(profile.getStudentId());
        double weighted = 0;
        double confidence = 0;
        for (LearnerSkillState state : states) {
            if (state.getMastery() == null || state.getConfidence() == null) continue;
            double weight = state.getConfidence().doubleValue();
            weighted += state.getMastery().doubleValue() * weight;
            confidence += weight;
        }
        profile.setOverallMastery(confidence == 0 ? null : decimal(weighted / confidence, 2));
        profileRepository.save(profile);
    }

    private PracticeObservation observation(LearningEvent event) {
        return switch (event.getEventType()) {
            case VOCAB_REVIEWED -> new PracticeObservation(
                    LearnerSkill.VOCABULARY,
                    vocabularyScore(event.getPayload()),
                    properties.getVocabularyReviewReliability());
            case VOCAB_ROUND_COMPLETED -> new PracticeObservation(
                    LearnerSkill.VOCABULARY,
                    requireScore(event),
                    Math.min(1.0, event.getPayload().path("totalQuestions").asInt(10) / 10.0));
            case PRONUNCIATION_PRACTICED -> new PracticeObservation(
                    LearnerSkill.PRONUNCIATION,
                    requireScore(event),
                    properties.getPronunciationReliability());
            case SPEAKING_SESSION_EVALUATED -> new PracticeObservation(
                    LearnerSkill.SPEAKING,
                    requireScore(event),
                    properties.getSpeakingReliability());
            default -> throw new IllegalArgumentException(
                    "Unsupported learner observation: " + event.getEventType());
        };
    }

    private double vocabularyScore(JsonNode payload) {
        String rating = payload == null ? "" : payload.path("rating").asText("");
        return switch (rating) {
            case "AGAIN" -> 0;
            case "HARD" -> 25;
            case "FAIR" -> 50;
            case "GOOD" -> 75;
            case "EASY" -> 100;
            default -> throw new IllegalArgumentException("VOCAB_REVIEWED payload has invalid rating");
        };
    }

    private double requireScore(LearningEvent event) {
        if (event.getScore() == null) {
            throw new IllegalArgumentException(event.getEventType() + " is missing score");
        }
        return event.getScore();
    }

    private MasteryTrend trend(double recent, double mastery) {
        double difference = recent - mastery;
        if (difference >= 3) return MasteryTrend.UP;
        if (difference <= -3) return MasteryTrend.DOWN;
        return MasteryTrend.STABLE;
    }

    private BigDecimal decimal(double value, int scale) {
        return BigDecimal.valueOf(Math.max(0, Math.min(100, value)))
                .setScale(scale, RoundingMode.HALF_UP);
    }

    private BigDecimal scaled(double value, int scale) {
        return BigDecimal.valueOf(Math.max(0, value)).setScale(scale, RoundingMode.HALF_UP);
    }

    private CefrLevel parseCefr(String value) {
        return value == null || value.isBlank() ? null : CefrLevel.valueOf(value);
    }

    private LearnerProfileResponse toResponse(
            LearnerProfile profile,
            List<LearnerSkillState> states) {
        return LearnerProfileResponse.builder()
                .cefr(profile.getCefrLevel())
                .cefrSource(profile.getCefrSource())
                .cefrAssessedAt(profile.getCefrAssessedAt())
                .overallMastery(profile.getOverallMastery())
                .profileVersion(profile.getProfileVersion())
                .totalActivities(profile.getTotalActivities())
                .skills(states.stream().map(state -> LearnerSkillResponse.builder()
                        .skill(state.getSkill())
                        .cefrLevel(state.getCefrLevel())
                        .source(state.getSource())
                        .mastery(state.getMastery())
                        .recentScore(state.getRecentScore())
                        .confidence(state.getConfidence())
                        .trend(state.getTrend())
                        .evidenceCount(state.getEvidenceCount())
                        .lastPracticedAt(state.getLastPracticedAt())
                        .build()).toList())
                .build();
    }

    private record PlacementSkill(Short score, CefrLevel cefr) {
    }

    private record PracticeObservation(LearnerSkill skill, double score, double reliability) {
    }
}
