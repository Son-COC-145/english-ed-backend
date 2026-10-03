package com.example.english_app.service.adaptive.event;

import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventStatus;
import com.example.english_app.repository.adaptive.LearningEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningEventProcessor {

    private final LearningEventRepository eventRepository;
    private final LearningEventRouter router;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(UUID eventId, String correlationId, LocalDateTime processedAt) {
        LearningEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Claimed learning event does not exist"));
        if (event.getStatus() != LearningEventStatus.PROCESSING
                || !Objects.equals(event.getCorrelationId(), correlationId)) {
            return;
        }

        router.route(event);
        event.setStatus(LearningEventStatus.DONE);
        event.setProcessedAt(processedAt);
        event.setLastError(null);
        event.setCorrelationId(null);
        event.setProcessingStartedAt(null);
    }
}
