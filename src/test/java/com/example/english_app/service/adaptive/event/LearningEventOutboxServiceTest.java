package com.example.english_app.service.adaptive.event;

import com.example.english_app.dto.request.adaptive.LearningEventRequest;
import com.example.english_app.entity.enums.LearningEventSource;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.event.LearningEventCreatedEvent;
import com.example.english_app.repository.adaptive.LearningEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningEventOutboxServiceTest {

    @Mock private LearningEventRepository eventRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @Test
    void savesPendingEvent() {
        LearningEventOutboxService service = service();
        LearningEventRequest request = request();
        when(eventRepository.insertIfAbsent(
                any(), anyLong(), anyString(), anyString(), anyString(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyString(), any(), any(), isNull(), any()))
                .thenReturn(1);

        boolean inserted = service.saveOutbox(request);

        assertThat(inserted).isTrue();
        verify(eventRepository).insertIfAbsent(
                any(), eq(7L), eq("VOCAB_REVIEWED"), eq("VOCAB_REVIEW"), eq("attempt-1"),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                eq("{}"), eq((short) 1), any(), isNull(), any());
        verify(eventPublisher).publishEvent(any(LearningEventCreatedEvent.class));
    }

    @Test
    void returnsFalseForDuplicateEvent() {
        LearningEventOutboxService service = service();
        when(eventRepository.insertIfAbsent(
                any(), anyLong(), anyString(), anyString(), anyString(),
                isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
                anyString(), any(), any(), isNull(), any()))
                .thenReturn(0);

        assertThat(service.saveOutbox(request())).isFalse();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void requestDeclaresItsValidationRules() {
        LearningEventRequest invalid = LearningEventRequest.builder()
                .studentId(7L)
                .eventType(LearningEventType.VOCAB_REVIEWED)
                .source(LearningEventSource.VOCAB_REVIEW)
                .sourceReference(" ")
                .build();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertThat(validator.validate(invalid))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("sourceReference"));
    }

    @Test
    void requiresExistingBusinessTransaction() throws Exception {
        Method method = LearningEventOutboxService.class
                .getMethod("saveOutbox", LearningEventRequest.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.MANDATORY);
    }

    private LearningEventOutboxService service() {
        return new LearningEventOutboxService(eventRepository, new ObjectMapper(), eventPublisher);
    }

    private LearningEventRequest request() {
        return LearningEventRequest.builder()
                .studentId(7L)
                .eventType(LearningEventType.VOCAB_REVIEWED)
                .source(LearningEventSource.VOCAB_REVIEW)
                .sourceReference("attempt-1")
                .build();
    }
}
