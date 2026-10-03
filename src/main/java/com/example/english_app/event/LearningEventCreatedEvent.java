package com.example.english_app.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@Getter
@RequiredArgsConstructor
public class LearningEventCreatedEvent {

    private final UUID eventId;
}
