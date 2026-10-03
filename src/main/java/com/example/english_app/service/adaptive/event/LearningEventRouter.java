package com.example.english_app.service.adaptive.event;

import com.example.english_app.entity.adaptive.LearningEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class LearningEventRouter {

    private final List<LearningEventConsumer> consumers;

    public LearningEventRouter(List<LearningEventConsumer> consumers) {
        List<LearningEventConsumer> ordered = new ArrayList<>(consumers);
        ordered.sort(Comparator.comparing(LearningEventConsumer::stage));
        this.consumers = List.copyOf(ordered);
    }

    public void route(LearningEvent event) {
        for (LearningEventConsumer consumer : consumers) {
            if (consumer.supports(event.getEventType())) {
                consumer.apply(event);
            }
        }
    }
}
