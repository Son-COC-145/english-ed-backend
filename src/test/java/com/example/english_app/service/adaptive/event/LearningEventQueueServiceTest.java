package com.example.english_app.service.adaptive.event;

import com.example.english_app.config.AdaptiveWorkerProperties;
import com.example.english_app.repository.adaptive.LearningEventQueueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LearningEventQueueServiceTest {

    @Mock private LearningEventQueueStore queueStore;

    private AdaptiveWorkerProperties properties;
    private LearningEventQueueService service;

    @BeforeEach
    void setUp() {
        properties = new AdaptiveWorkerProperties();
        properties.setBatchSize(25);
        properties.setMaxAttempts(4);
        properties.setMaxBackoffSeconds(120);
        properties.setStuckAfterMs(60_000);
        service = new LearningEventQueueService(queueStore, properties);
    }

    @Test
    void appliesConfiguredClaimLimit() {
        LocalDateTime now = LocalDateTime.now();

        service.claimBatch("claim-1", now);

        verify(queueStore).claimPending("claim-1", 25, now);
    }

    @Test
    void recordsOnlyFailureTypeAndRetrySettings() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        service.markFailure(eventId, "claim-1", new IllegalStateException("secret"), now);

        verify(queueStore).markRetryOrFailed(
                eventId, "claim-1", 4, 120, "IllegalStateException", now);
    }

    @Test
    void releasesClaimsOlderThanConfiguredTimeout() {
        LocalDateTime now = LocalDateTime.now();

        service.releaseStuck(now);

        verify(queueStore).releaseStuck(now.minusSeconds(60), now);
    }
}
