package com.example.english_app.service.adaptive.event;

import com.example.english_app.config.AdaptiveWorkerProperties;
import com.example.english_app.repository.adaptive.LearningEventQueueStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningEventQueueService {

    private final LearningEventQueueStore queueStore;
    private final AdaptiveWorkerProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<UUID> claimBatch(String correlationId, LocalDateTime now) {
        return queueStore.claimPending(correlationId, properties.getBatchSize(), now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailure(
            UUID eventId,
            String correlationId,
            Throwable failure,
            LocalDateTime now) {
        queueStore.markRetryOrFailed(
                eventId,
                correlationId,
                properties.getMaxAttempts(),
                properties.getMaxBackoffSeconds(),
                failure.getClass().getSimpleName(),
                now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int releaseStuck(LocalDateTime now) {
        LocalDateTime stuckBefore = now.minusNanos(properties.getStuckAfterMs() * 1_000_000);
        return queueStore.releaseStuck(stuckBefore, now);
    }
}
