package com.example.english_app.service.adaptive.learner;

import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.service.adaptive.event.LearningEventConsumer;
import com.example.english_app.service.adaptive.event.LearningEventConsumerStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class LearnerModelConsumer implements LearningEventConsumer {

    private static final Set<LearningEventType> PRACTICE_EVENTS = EnumSet.of(
            LearningEventType.VOCAB_REVIEWED,
            LearningEventType.VOCAB_ROUND_COMPLETED,
            LearningEventType.PRONUNCIATION_PRACTICED,
            LearningEventType.SPEAKING_SESSION_EVALUATED);

    private final LearnerProfileService learnerProfileService;

    @Override
    public LearningEventConsumerStage stage() {
        return LearningEventConsumerStage.LEARNER_MODEL;
    }

    @Override
    public boolean supports(LearningEventType eventType) {
        return eventType == LearningEventType.PLACEMENT_COMPLETED
                || PRACTICE_EVENTS.contains(eventType);
    }

    @Override
    public void apply(LearningEvent event) {
        if (event.getEventType() == LearningEventType.PLACEMENT_COMPLETED) {
            learnerProfileService.applyPlacement(event);
        } else {
            learnerProfileService.applyPractice(event);
        }
    }
}
