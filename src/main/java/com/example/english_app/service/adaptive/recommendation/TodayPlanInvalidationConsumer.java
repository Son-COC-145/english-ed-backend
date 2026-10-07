package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.config.AppTimeZone;
import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.repository.adaptive.TodayPlanRepository;
import com.example.english_app.service.adaptive.event.LearningEventConsumer;
import com.example.english_app.service.adaptive.event.LearningEventConsumerStage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TodayPlanInvalidationConsumer implements LearningEventConsumer {

    private static final Set<LearningEventType> SUPPORTED = EnumSet.of(
            LearningEventType.PLACEMENT_COMPLETED,
            LearningEventType.VOCAB_REVIEWED,
            LearningEventType.VOCAB_ROUND_COMPLETED,
            LearningEventType.PRONUNCIATION_PRACTICED,
            LearningEventType.SPEAKING_SESSION_EVALUATED,
            LearningEventType.ASSIGNMENT_GRADED,
            LearningEventType.ROADMAP_GENERATED,
            LearningEventType.ROADMAP_MODULE_COMPLETED,
            LearningEventType.ROADMAP_WEEK_COMPLETED,
            LearningEventType.ROADMAP_COMPLETED);

    private final TodayPlanRepository planRepository;

    @Override
    public LearningEventConsumerStage stage() {
        return LearningEventConsumerStage.PLAN_CACHE;
    }

    @Override
    public boolean supports(LearningEventType eventType) {
        return SUPPORTED.contains(eventType);
    }

    @Override
    public void apply(LearningEvent event) {
        LocalDateTime now = LocalDateTime.now(AppTimeZone.ZONE);
        planRepository.markDirty(event.getStudentId(), LocalDate.now(AppTimeZone.ZONE), now);
    }
}
