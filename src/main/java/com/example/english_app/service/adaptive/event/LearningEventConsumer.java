package com.example.english_app.service.adaptive.event;

import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventType;

public interface LearningEventConsumer {

    LearningEventConsumerStage stage();

    boolean supports(LearningEventType eventType);

    void apply(LearningEvent event);
}
