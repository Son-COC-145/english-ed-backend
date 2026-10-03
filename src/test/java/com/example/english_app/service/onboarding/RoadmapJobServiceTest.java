package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.request.adaptive.LearningEventRequest;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.onboarding.RoadmapGenerationJobRepository;
import com.example.english_app.service.adaptive.event.LearningEventOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapJobServiceTest {

    @Mock private RoadmapGenerationJobRepository jobRepository;
    @Mock private OnboardingRepository onboardingRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher publisher;
    @Mock private LearningEventOutboxService learningEventOutboxService;
    private RoadmapJobService service;

    @BeforeEach
    void setUp() {
        service = new RoadmapJobService(
                jobRepository,
                onboardingRepository,
                new ObjectMapper(),
                publisher,
                learningEventOutboxService);
    }

    @Test
    void claimBatchMarksAttemptAndVisibleProcessingState() {
        RoadmapGenerationJob job = job(1);
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .roadmapGenerationVersion(1).build();
        when(jobRepository.lockDispatchable(
                eq(1), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(job));
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.of(onboarding));

        List<RoadmapGenerationJob> claimed = service.claimBatch(1);

        assertThat(claimed).containsExactly(job);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getClaimToken()).isNotBlank();
        assertThat(job.getLockedAt()).isNotNull();
        assertThat(onboarding.getRoadmapStatus()).isEqualTo(RoadmapGenerationStatus.PROCESSING);
        assertThat(onboarding.getRoadmapGenerationAttempts()).isEqualTo(1);

        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> staleBeforeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jobRepository).lockDispatchable(
                eq(1), nowCaptor.capture(), staleBeforeCaptor.capture());
        assertThat(job.getLockedAt()).isEqualTo(nowCaptor.getValue());
        assertThat(staleBeforeCaptor.getValue()).isEqualTo(nowCaptor.getValue().minusMinutes(5));
    }

    @Test
    void enqueuePublishesImmediateWakeupUsingPersistedAvailability() {
        when(jobRepository.existsByStudentIdAndGenerationVersion(7L, 1)).thenReturn(false);

        service.enqueue(7L, 1, CefrLevel.B1, "{}");

        ArgumentCaptor<RoadmapGenerationJob> jobCaptor = ArgumentCaptor.forClass(RoadmapGenerationJob.class);
        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(jobRepository).save(jobCaptor.capture());
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(RoadmapJobWakeupEvent.class);
        RoadmapJobWakeupEvent event = (RoadmapJobWakeupEvent) eventCaptor.getValue();
        assertThat(event.getAvailableAt()).isEqualTo(jobCaptor.getValue().getAvailableAt());
    }

    @Test
    void onlyCurrentClaimTokenCanPublishReadyRoadmap() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("claim");
        job.setAttemptCount(1);
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .roadmapGenerationVersion(1).build();
        when(jobRepository.completeClaim(
                eq(11L), eq("claim"), eq(RoadmapGenerationStatus.READY),
                eq(null), eq(1), any(LocalDateTime.class))).thenReturn(1);
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.of(onboarding));

        service.markReady(job, validRoadmap());

        assertThat(onboarding.getRoadmapStatus()).isEqualTo(RoadmapGenerationStatus.READY);
        assertThat(onboarding.getRoadmapJson()).contains("\"cefrLevel\":\"B1\"");
        ArgumentCaptor<LearningEventRequest> eventCaptor =
                ArgumentCaptor.forClass(LearningEventRequest.class);
        verify(learningEventOutboxService).saveOutbox(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType())
                .isEqualTo(LearningEventType.ROADMAP_GENERATED);
        assertThat(eventCaptor.getValue().getSourceReference()).isEqualTo("7:1");
    }

    @Test
    void staleClaimCannotOverwriteOnboarding() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("stale");
        job.setAttemptCount(1);
        when(jobRepository.completeClaim(
                eq(11L), eq("stale"), eq(RoadmapGenerationStatus.READY),
                eq(null), eq(1), any(LocalDateTime.class))).thenReturn(0);

        service.markReady(job, validRoadmap());

        verify(onboardingRepository, never()).findByStudentId(any());
    }

    @Test
    void staleFailedClaimCannotOverwriteOnboarding() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("stale");
        job.setAttemptCount(1);
        when(jobRepository.completeClaim(
                eq(11L), eq("stale"), eq(RoadmapGenerationStatus.PENDING),
                eq("IllegalStateException"), eq(1), any(LocalDateTime.class))).thenReturn(0);

        service.markFailure(job, new IllegalStateException("generation failed"));

        verify(onboardingRepository, never()).findByStudentId(any());
        verify(publisher, never()).publishEvent(any(ApplicationEvent.class));
    }

    @Test
    void emptyRoadmapCannotTransitionJobOrOnboardingToReady() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("claim");
        job.setAttemptCount(1);
        RoadmapResponse empty = RoadmapResponse.builder()
                .cefrLevel("B1")
                .totalWeeks(0)
                .milestones(List.of())
                .build();

        assertThatThrownBy(() -> service.markReady(job, empty))
                .isInstanceOf(RoadmapContentUnavailableException.class)
                .hasMessageContaining("no milestones");

        verify(jobRepository, never()).completeClaim(
                any(), any(), any(), any(), anyInt(), any(LocalDateTime.class));
        verify(onboardingRepository, never()).findByStudentId(any());
    }

    @Test
    void moduleWithoutContentSnapshotCannotTransitionToReady() {
        RoadmapGenerationJob job = job(1);
        RoadmapResponse invalid = RoadmapResponse.builder()
                .schemaVersion(2)
                .currentCefrLevel("B1")
                .targetCefrLevel("B2")
                .cefrLevel("B1")
                .totalWeeks(1)
                .milestones(List.of(RoadmapMilestone.builder()
                        .weekNumber(1)
                        .modules(List.of(RoadmapModule.builder()
                                .moduleKey("VOCABULARY:1")
                                .contentVersion("v1")
                                .contentItemIds(List.of())
                                .itemCount(0)
                                .build()))
                        .build()))
                .build();

        assertThatThrownBy(() -> service.markReady(job, invalid))
                .isInstanceOf(RoadmapContentUnavailableException.class)
                .hasMessageContaining("valid content snapshot");

        verify(jobRepository, never()).completeClaim(
                any(), any(), any(), any(), anyInt(), any(LocalDateTime.class));
    }

    @Test
    void retryableFailurePublishesDelayedWakeupAfterSuccessfulClaimCompletion() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("claim");
        job.setAttemptCount(1);
        when(jobRepository.completeClaim(
                eq(11L), eq("claim"), eq(RoadmapGenerationStatus.PENDING),
                eq("IllegalStateException"), eq(1), any(LocalDateTime.class))).thenReturn(1);

        service.markFailure(job, new IllegalStateException("generation failed"));

        ArgumentCaptor<LocalDateTime> availableAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jobRepository).completeClaim(
                eq(11L), eq("claim"), eq(RoadmapGenerationStatus.PENDING),
                eq("IllegalStateException"), eq(1), availableAtCaptor.capture());
        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(RoadmapJobWakeupEvent.class);
        assertThat(((RoadmapJobWakeupEvent) eventCaptor.getValue()).getAvailableAt())
                .isEqualTo(availableAtCaptor.getValue());
    }

    @Test
    void terminalFailureDoesNotScheduleAnotherWakeup() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("claim");
        job.setAttemptCount(5);
        when(jobRepository.completeClaim(
                eq(11L), eq("claim"), eq(RoadmapGenerationStatus.FAILED),
                eq("IllegalStateException"), eq(5), any(LocalDateTime.class))).thenReturn(1);

        service.markFailure(job, new IllegalStateException("generation failed"));

        verify(publisher, never()).publishEvent(any(ApplicationEvent.class));
    }

    @Test
    void manualRetryResetsFailedJobAndPublishesImmediateWakeup() {
        RoadmapGenerationJob job = job(1);
        job.setStatus(RoadmapGenerationStatus.FAILED);
        job.setAttemptCount(5);
        job.setLastError("IllegalStateException");
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .placementCefrLevel(CefrLevel.B1)
                .roadmapGenerationVersion(1)
                .roadmapStatus(RoadmapGenerationStatus.FAILED)
                .roadmapGenerationAttempts(5)
                .roadmapLastError("IllegalStateException")
                .build();
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.of(onboarding));
        when(jobRepository.findTopByStudentIdOrderByGenerationVersionDesc(7L)).thenReturn(Optional.of(job));

        service.retry(7L);

        assertThat(job.getStatus()).isEqualTo(RoadmapGenerationStatus.PENDING);
        assertThat(job.getAttemptCount()).isZero();
        assertThat(job.getLastError()).isNull();
        assertThat(onboarding.getRoadmapStatus()).isEqualTo(RoadmapGenerationStatus.PENDING);
        assertThat(onboarding.getRoadmapGenerationAttempts()).isZero();

        ArgumentCaptor<ApplicationEvent> eventCaptor = ArgumentCaptor.forClass(ApplicationEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(RoadmapJobWakeupEvent.class);
        assertThat(((RoadmapJobWakeupEvent) eventCaptor.getValue()).getAvailableAt())
                .isEqualTo(job.getAvailableAt());
    }

    @Test
    void jobFromBeforeResetCannotPublishIntoNewGeneration() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("claim");
        job.setAttemptCount(1);
        StudentOnboarding resetOnboarding = StudentOnboarding.builder()
                .roadmapGenerationVersion(2)
                .build();
        when(jobRepository.completeClaim(
                eq(11L), eq("claim"), eq(RoadmapGenerationStatus.READY),
                eq(null), eq(1), any(LocalDateTime.class))).thenReturn(1);
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.of(resetOnboarding));

        service.markReady(job, validRoadmap());

        assertThat(resetOnboarding.getRoadmapStatus()).isNull();
        assertThat(resetOnboarding.getRoadmapJson()).isNull();
    }

    private RoadmapGenerationJob job(int version) {
        return RoadmapGenerationJob.builder()
                .id(11L)
                .studentId(7L)
                .generationVersion(version)
                .cefrLevel(CefrLevel.B1)
                .status(RoadmapGenerationStatus.PENDING)
                .attemptCount(0)
                .availableAt(LocalDateTime.now())
                .build();
    }

    private RoadmapResponse validRoadmap() {
        return RoadmapResponse.builder()
                .schemaVersion(2)
                .currentCefrLevel("B1")
                .targetCefrLevel("B2")
                .cefrLevel("B1")
                .totalWeeks(1)
                .milestones(List.of(RoadmapMilestone.builder()
                        .weekNumber(1)
                        .modules(List.of(RoadmapModule.builder()
                                .type("VOCABULARY")
                                .moduleKey("VOCABULARY:1")
                                .contentItemIds(List.of(10L))
                                .contentVersion("snapshot-v1")
                                .itemCount(1)
                                .build()))
                        .build()))
                .build();
    }
}
