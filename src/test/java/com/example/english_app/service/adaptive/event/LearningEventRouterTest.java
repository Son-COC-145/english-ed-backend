package com.example.english_app.service.adaptive.event;

import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LearningEventRouterTest {

    @Test
    void routesOnlySupportingConsumersInFixedOrder() {
        List<String> calls = new ArrayList<>();
        LearningEventConsumer planCache = consumer(
                "plan-cache", LearningEventConsumerStage.PLAN_CACHE, true, calls);
        LearningEventConsumer learnerModel = consumer(
                "learner-model", LearningEventConsumerStage.LEARNER_MODEL, true, calls);
        LearningEventConsumer unsupported = consumer(
                "unsupported", LearningEventConsumerStage.ROADMAP_PROGRESS, false, calls);
        LearningEvent event = LearningEvent.builder()
                .eventType(LearningEventType.VOCAB_REVIEWED)
                .build();

        new LearningEventRouter(List.of(planCache, learnerModel, unsupported)).route(event);

        assertThat(calls).containsExactly("learner-model", "plan-cache");
    }

    private LearningEventConsumer consumer(
            String name,
            LearningEventConsumerStage stage,
            boolean supports,
            List<String> calls) {
        return new LearningEventConsumer() {
            @Override
            public LearningEventConsumerStage stage() {
                return stage;
            }

            @Override
            public boolean supports(LearningEventType eventType) {
                return supports;
            }

            @Override
            public void apply(LearningEvent event) {
                calls.add(name);
            }
        };
    }
}
