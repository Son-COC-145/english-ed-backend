package com.example.english_app.scheduler;

import com.example.english_app.event.LearningEventCreatedEvent;
import com.example.english_app.service.adaptive.event.LearningEventProcessor;
import com.example.english_app.service.adaptive.event.LearningEventQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class LearningEventWorker {

    private final LearningEventQueueService queueService;
    private final LearningEventProcessor processor;
    private final AtomicBoolean running = new AtomicBoolean();

    @Scheduled(fixedDelayString = "${adaptive.worker.poll-ms:5000}")
    public void poll() {
        runSafely();
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void wakeUp(LearningEventCreatedEvent ignored) {
        runSafely();
    }

    void runSafely() {
        if (!running.compareAndSet(false, true)) return;
        try {
            LocalDateTime now = LocalDateTime.now();
            int released = queueService.releaseStuck(now);
            if (released > 0) {
                log.warn("Released {} stuck learning events", released);
            }

            String correlationId = UUID.randomUUID().toString();
            List<UUID> eventIds = queueService.claimBatch(correlationId, now);
            for (UUID eventId : eventIds) {
                try {
                    processor.process(eventId, correlationId, LocalDateTime.now());
                } catch (RuntimeException failure) {
                    log.warn("Learning event processing failed: eventId={}", eventId, failure);
                    queueService.markFailure(
                            eventId, correlationId, failure, LocalDateTime.now());
                }
            }
        } catch (RuntimeException failure) {
            log.error("Cannot dispatch learning events", failure);
        } finally {
            running.set(false);
        }
    }
}
