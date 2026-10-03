package com.example.english_app.service.adaptive.event;

import com.example.english_app.dto.request.adaptive.LearningEventRequest;
import com.example.english_app.event.LearningEventCreatedEvent;
import com.example.english_app.repository.adaptive.LearningEventRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Validated
@RequiredArgsConstructor
public class LearningEventOutboxService {

    private static final short DEFAULT_SCHEMA_VERSION = 1;

    private final LearningEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean saveOutbox(@NotNull @Valid LearningEventRequest request) {
        UUID eventId = UUID.randomUUID();
        LocalDateTime occurredAt = request.getOccurredAt() == null
                ? LocalDateTime.now()
                : request.getOccurredAt();
        JsonNode payload = request.getPayload() == null
                ? objectMapper.createObjectNode()
                : request.getPayload();
        short schemaVersion = request.getSchemaVersion() == null
                ? DEFAULT_SCHEMA_VERSION
                : request.getSchemaVersion();

        int inserted = eventRepository.insertIfAbsent(
                eventId,
                request.getStudentId(),
                request.getEventType().name(),
                request.getSource().name(),
                request.getSourceReference(),
                request.getSkill() == null ? null : request.getSkill().name(),
                blankToNull(request.getEntityType()),
                request.getEntityId(),
                request.getScore(),
                request.getDurationSeconds(),
                request.getDurationSource() == null ? null : request.getDurationSource().name(),
                payload.toString(),
                schemaVersion,
                occurredAt,
                request.getCausationEventId(),
                occurredAt);

        if (inserted == 1) {
            eventPublisher.publishEvent(new LearningEventCreatedEvent(eventId));
            return true;
        }
        if (inserted == 0) return false;
        throw new IllegalStateException("Learning event insert affected " + inserted + " rows");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
