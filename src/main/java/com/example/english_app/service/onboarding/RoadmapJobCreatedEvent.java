package com.example.english_app.service.onboarding;

import org.springframework.context.ApplicationEvent;

public class RoadmapJobCreatedEvent extends ApplicationEvent {
    public RoadmapJobCreatedEvent(Object source) {
        super(source);
    }
}
