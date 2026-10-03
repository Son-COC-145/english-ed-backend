package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.dto.request.adaptive.LearningEventRequest;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.service.adaptive.event.LearningEventOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapProgressConsumerTest {

    @Mock private OnboardingRepository onboardingRepository;
    @Mock private RoadmapProgressService progressService;
    @Mock private LearningEventOutboxService outboxService;

    private RoadmapProgressConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new RoadmapProgressConsumer(
                onboardingRepository,
                progressService,
                outboxService,
                new ObjectMapper());
    }

    @Test
    void recalculatesAndPublishesIdempotentCompletionEvents() {
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .roadmapJson("present")
                .roadmapGenerationVersion(3)
                .build();
        RoadmapModule module = RoadmapModule.builder()
                .moduleKey("VOCABULARY:12")
                .status(RoadmapItemStatus.COMPLETED)
                .build();
        RoadmapMilestone milestone = RoadmapMilestone.builder()
                .weekNumber(2)
                .status(RoadmapItemStatus.COMPLETED)
                .modules(List.of(module))
                .build();
        RoadmapProgressResponse progress = RoadmapProgressResponse.builder()
                .roadmapVersion(3)
                .completed(true)
                .milestones(List.of(milestone))
                .build();
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.of(onboarding));
        when(progressService.recalculateAll(onboarding)).thenReturn(
                new RoadmapProgressService.RoadmapProgressResult(
                        RoadmapResponse.builder().build(), progress));

        LearningEvent cause = LearningEvent.builder()
                .eventId(UUID.randomUUID())
                .studentId(7L)
                .eventType(LearningEventType.VOCAB_REVIEWED)
                .build();
        consumer.apply(cause);

        ArgumentCaptor<LearningEventRequest> requests =
                ArgumentCaptor.forClass(LearningEventRequest.class);
        verify(outboxService, org.mockito.Mockito.times(3)).saveOutbox(requests.capture());
        assertThat(requests.getAllValues())
                .extracting(LearningEventRequest::getEventType)
                .containsExactly(
                        LearningEventType.ROADMAP_MODULE_COMPLETED,
                        LearningEventType.ROADMAP_WEEK_COMPLETED,
                        LearningEventType.ROADMAP_COMPLETED);
        List<String> sourceReferences = requests.getAllValues().stream()
                .map(LearningEventRequest::getSourceReference)
                .toList();
        assertThat(sourceReferences.getFirst())
                .startsWith("7:3:MODULE:")
                .hasSizeLessThanOrEqualTo(120);
        assertThat(sourceReferences).containsExactly(
                sourceReferences.getFirst(),
                "7:3:WEEK:2",
                "7:3:ALL");
        assertThat(requests.getAllValues())
                .allSatisfy(request -> assertThat(request.getCausationEventId())
                        .isEqualTo(cause.getEventId()));
    }

    @Test
    void ignoresLearningActivityBeforeRoadmapExists() {
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.empty());
        LearningEvent event = LearningEvent.builder()
                .eventId(UUID.randomUUID())
                .studentId(7L)
                .eventType(LearningEventType.VOCAB_REVIEWED)
                .build();

        consumer.apply(event);

        verify(progressService, never()).recalculateAll(org.mockito.ArgumentMatchers.any(
                StudentOnboarding.class));
        verify(outboxService, never()).saveOutbox(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void supportsOnlyEventsThatCanChangeRoadmapProgress() {
        assertThat(consumer.supports(LearningEventType.ROADMAP_GENERATED)).isTrue();
        assertThat(consumer.supports(LearningEventType.PRONUNCIATION_PRACTICED)).isTrue();
        assertThat(consumer.supports(LearningEventType.ROADMAP_MODULE_COMPLETED)).isFalse();
        assertThat(consumer.supports(LearningEventType.ASSIGNMENT_GRADED)).isFalse();
    }
}
