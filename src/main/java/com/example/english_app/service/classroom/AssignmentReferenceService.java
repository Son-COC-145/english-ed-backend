package com.example.english_app.service.classroom;

import com.example.english_app.dto.response.classroom.SubmissionResultResponse;
import com.example.english_app.entity.classroom.Assignment;
import com.example.english_app.entity.enums.MinigameRoundStatus;
import com.example.english_app.entity.enums.ModuleType;
import com.example.english_app.entity.enums.PracticeType;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.ipa.IpaExampleWordRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.vocabulary.TopicRepository;
import com.example.english_app.repository.gamification.MinigameRoundRepository;
import com.example.english_app.repository.speaking.SpeakingScenarioRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentReferenceService {
    private final IpaExampleWordRepository wordRepository;
    private final TopicRepository topicRepository;
    private final SpeakingScenarioRepository scenarioRepository;
    private final PronunciationPracticeLogRepository practiceRepository;
    /** VOCABULARY results are whole mini-game rounds, not single answers. */
    private final MinigameRoundRepository roundRepository;
    private final SpeakingSessionRepository sessionRepository;

    /** Practice logs whose refId points to an IPA example word; IPA practice currently writes IPA_PHONEME. */
    private static final Set<PracticeType> WORD_PRACTICE_TYPES = EnumSet.of(PracticeType.WORD, PracticeType.IPA_PHONEME);

    public void validateTarget(ModuleType moduleType, Long refId) {
        if (moduleType == null || refId == null || refId <= 0) throw ErrorCode.INVALID_REQUEST.toException();
        boolean exists = switch (moduleType) {
            case PRONUNCIATION -> wordRepository.existsById(refId);
            case VOCABULARY -> refId <= Short.MAX_VALUE && topicRepository.existsById(refId.shortValue());
            case SPEAKING -> refId <= Short.MAX_VALUE && scenarioRepository.existsById(refId.shortValue());
        };
        if (!exists) throw ErrorCode.INVALID_REQUEST.toException();
    }

    public void validateResult(Assignment assignment, Long studentId, Long resultId) {
        if (resultId == null || resultId <= 0) throw ErrorCode.INVALID_REQUEST.toException();
        boolean matches = switch (assignment.getModuleType()) {
            case PRONUNCIATION -> practiceRepository.findById(resultId)
                    .filter(result -> result.getStudent().getId().equals(studentId)
                            && WORD_PRACTICE_TYPES.contains(result.getPracticeType())
                            && result.getRefId().equals(assignment.getRefId())).isPresent();
            case VOCABULARY -> roundRepository.findById(resultId)
                    .filter(round -> round.getStudent().getId().equals(studentId)
                            && round.getTopic().getId().longValue() == assignment.getRefId()
                            && round.getStatus() == MinigameRoundStatus.COMPLETED).isPresent();
            case SPEAKING -> sessionRepository.findById(resultId)
                    .filter(result -> result.getStudent().getId().equals(studentId)
                            && result.getScenario().getId().longValue() == assignment.getRefId()
                            && "COMPLETED".equals(result.getStatus())).isPresent();
        };
        if (!matches) throw ErrorCode.INVALID_REQUEST.toException();
    }

    /** Loads the learning result behind a submission for teacher review; empty if it was removed. */
    public Optional<SubmissionResultResponse> describeResult(ModuleType moduleType, Long resultId) {
        if (moduleType == null || resultId == null) {
            return Optional.empty();
        }
        return switch (moduleType) {
            case PRONUNCIATION -> practiceRepository.findById(resultId).map(log -> SubmissionResultResponse.builder()
                    .moduleType(moduleType)
                    .resultId(log.getId())
                    .completedAt(log.getPracticedAt())
                    .overallScore(toInteger(log.getOverallScore()))
                    .fluencyScore(toInteger(log.getFluencyScore()))
                    .completenessScore(toInteger(log.getCompletenessScore()))
                    .stressCorrect(log.getStressCorrect())
                    .studentAudioUrl(log.getAudioUrl())
                    .build());
            case VOCABULARY -> roundRepository.findById(resultId).map(round -> SubmissionResultResponse.builder()
                    .moduleType(moduleType)
                    .resultId(round.getId())
                    .completedAt(round.getCompletedAt())
                    .overallScore(toInteger(round.getScore()))
                    .correctCount(round.getCorrectCount())
                    .totalQuestions(round.getTotalQuestions())
                    .gameType(round.getGameType() != null ? round.getGameType().name() : null)
                    .durationSeconds(round.getDurationSeconds())
                    .xpEarned(round.getXpEarned())
                    .build());
            case SPEAKING -> sessionRepository.findById(resultId).map(session -> SubmissionResultResponse.builder()
                    .moduleType(moduleType)
                    .resultId(session.getId())
                    .completedAt(session.getEndedAt())
                    .taskCompletionScore(toInteger(session.getTaskCompletionScore()))
                    .fluencyScore(toInteger(session.getFluencyScore()))
                    .intonationScore(toInteger(session.getIntonationScore()))
                    .xpEarned(toInteger(session.getXpEarned()))
                    .build());
        };
    }

    private static Integer toInteger(Short value) {
        return value != null ? value.intValue() : null;
    }
}
