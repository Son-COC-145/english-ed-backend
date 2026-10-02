package com.example.english_app.service.onboarding;

/** Raised when a generated roadmap has no usable, snapshotted learning content. */
public class RoadmapContentUnavailableException extends RuntimeException {

    public RoadmapContentUnavailableException(String message) {
        super(message);
    }
}
