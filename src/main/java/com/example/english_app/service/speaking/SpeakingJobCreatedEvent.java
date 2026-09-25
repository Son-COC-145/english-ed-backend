package com.example.english_app.service.speaking;

import org.springframework.context.ApplicationEvent;

public class SpeakingJobCreatedEvent extends ApplicationEvent {
    public SpeakingJobCreatedEvent(Object source) {
        super(source);
    }
}
