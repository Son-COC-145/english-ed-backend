package com.example.english_app.service.adaptive.event;

import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventStatus;
import com.example.english_app.repository.adaptive.LearningEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningEventProcessorTest {

    @Mock private LearningEventRepository eventRepository;
    @Mock private LearningEventRouter router;

    @Test
    void marksClaimedEventDoneAfterConsumersSucceed() {
        UUID eventId = UUID.randomUUID();
        LocalDateTime processedAt = LocalDateTime.now();
        LearningEvent event = processingEvent("claim-1");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        new LearningEventProcessor(eventRepository, router)
                .process(eventId, "claim-1", processedAt);

        verify(router).route(event);
        assertThat(event.getStatus()).isEqualTo(LearningEventStatus.DONE);
        assertThat(event.getProcessedAt()).isEqualTo(processedAt);
        assertThat(event.getCorrelationId()).isNull();
    }

    @Test
    void leavesEventProcessingWhenConsumerFails() {
        UUID eventId = UUID.randomUUID();
        LearningEvent event = processingEvent("claim-1");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        doThrow(new IllegalStateException("consumer failed")).when(router).route(event);

        assertThatThrownBy(() -> new LearningEventProcessor(eventRepository, router)
                .process(eventId, "claim-1", LocalDateTime.now()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(event.getStatus()).isEqualTo(LearningEventStatus.PROCESSING);
    }

    @Test
    void ignoresAStaleClaim() {
        UUID eventId = UUID.randomUUID();
        LearningEvent event = processingEvent("new-claim");
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        new LearningEventProcessor(eventRepository, router)
                .process(eventId, "old-claim", LocalDateTime.now());

        verify(router, never()).route(event);
    }

    private LearningEvent processingEvent(String correlationId) {
        return LearningEvent.builder()
                .status(LearningEventStatus.PROCESSING)
                .correlationId(correlationId)
                .build();
    }
}
