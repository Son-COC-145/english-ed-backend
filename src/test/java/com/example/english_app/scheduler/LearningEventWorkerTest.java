package com.example.english_app.scheduler;

import com.example.english_app.service.adaptive.event.LearningEventProcessor;
import com.example.english_app.service.adaptive.event.LearningEventQueueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningEventWorkerTest {

    @Mock private LearningEventQueueService queueService;
    @Mock private LearningEventProcessor processor;

    @Test
    void processesEveryClaimedEvent() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(queueService.claimBatch(anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));

        new LearningEventWorker(queueService, processor).poll();

        verify(processor).process(eq(first), anyString(), any(LocalDateTime.class));
        verify(processor).process(eq(second), anyString(), any(LocalDateTime.class));
    }

    @Test
    void failedEventIsReturnedToRetryFlow() {
        UUID eventId = UUID.randomUUID();
        when(queueService.claimBatch(anyString(), any(LocalDateTime.class)))
                .thenReturn(List.of(eventId));
        doThrow(new IllegalStateException("failed"))
                .when(processor).process(
                        org.mockito.ArgumentMatchers.eq(eventId),
                        anyString(),
                        any(LocalDateTime.class));

        new LearningEventWorker(queueService, processor).poll();

        verify(queueService).markFailure(
                org.mockito.ArgumentMatchers.eq(eventId),
                anyString(),
                any(IllegalStateException.class),
                any(LocalDateTime.class));
    }
}
