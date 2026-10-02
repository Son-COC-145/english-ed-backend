package com.example.english_app.service.onboarding;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;

/**
 * Signals that the durable roadmap queue has work at (or after) {@code availableAt}.
 *
 * <p>The database remains the source of truth. This event only avoids frequent database polling;
 * the low-frequency scheduled sweep recovers work after a process restart or a lost event.</p>
 */
public class RoadmapJobWakeupEvent extends ApplicationEvent {

    private final LocalDateTime availableAt;

    public RoadmapJobWakeupEvent(Object source, LocalDateTime availableAt) {
        super(source);
        this.availableAt = availableAt;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }
}
