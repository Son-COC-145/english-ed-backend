package com.example.english_app.service.adaptive.activity;

import com.example.english_app.config.AdaptiveStudyTimeProperties;
import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.DurationSource;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.repository.adaptive.StudentDailyActivityRepository;
import com.example.english_app.service.adaptive.event.LearningEventConsumer;
import com.example.english_app.service.adaptive.event.LearningEventConsumerStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class StudyTimeConsumer implements LearningEventConsumer {

    private static final Set<LearningEventType> SUPPORTED_EVENTS = EnumSet.of(
            LearningEventType.VOCAB_REVIEWED,
            LearningEventType.VOCAB_ROUND_COMPLETED,
            LearningEventType.PRONUNCIATION_PRACTICED,
            LearningEventType.SPEAKING_SESSION_EVALUATED);

    private final StudentDailyActivityRepository activityRepository;
    private final AdaptiveStudyTimeProperties properties;

    @Override
    public LearningEventConsumerStage stage() {
        return LearningEventConsumerStage.STUDY_TIME;
    }

    @Override
    public boolean supports(LearningEventType eventType) {
        return SUPPORTED_EVENTS.contains(eventType);
    }

    @Override
    public void apply(LearningEvent event) {
        int seconds = event.getDurationSeconds() == null
                ? estimatedSeconds(event.getEventType())
                : event.getDurationSeconds();
        seconds = Math.max(0, Math.min(seconds, properties.getMaxSecondsPerActivity()));
        DurationSource source = event.getDurationSeconds() == null
                ? DurationSource.ESTIMATED
                : event.getDurationSource() == null ? DurationSource.MEASURED : event.getDurationSource();
        int measured = source == DurationSource.MEASURED ? seconds : 0;
        int estimated = source == DurationSource.ESTIMATED ? seconds : 0;
        int affected = activityRepository.accumulate(
                event.getStudentId(),
                event.getOccurredAt().toLocalDate(),
                measured,
                estimated,
                LocalDateTime.now());
        if (affected != 1) {
            throw new IllegalStateException("Study time upsert affected " + affected + " rows");
        }
    }

    private int estimatedSeconds(LearningEventType type) {
        return switch (type) {
            case VOCAB_REVIEWED -> properties.getVocabularyReviewedSeconds();
            case VOCAB_ROUND_COMPLETED -> properties.getVocabularyRoundSeconds();
            case PRONUNCIATION_PRACTICED -> properties.getPronunciationSeconds();
            case SPEAKING_SESSION_EVALUATED -> properties.getSpeakingSeconds();
            default -> throw new IllegalArgumentException("No study time estimate for " + type);
        };
    }
}
