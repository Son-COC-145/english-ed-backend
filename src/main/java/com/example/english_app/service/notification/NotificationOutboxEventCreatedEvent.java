package com.example.english_app.service.notification;

import org.springframework.context.ApplicationEvent;

public class NotificationOutboxEventCreatedEvent extends ApplicationEvent {
    public NotificationOutboxEventCreatedEvent(Object source) {
        super(source);
    }
}
