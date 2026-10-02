package com.example.english_app.service.onboarding;

import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.onboarding.RoadmapGenerationJobRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapJobServiceTest {

    @Mock private RoadmapGenerationJobRepository jobRepository;
    @Mock private OnboardingRepository onboardingRepository;
    @Mock private org.springframework.context.ApplicationEventPublisher publisher;
    private RoadmapJobService service;

    @BeforeEach
    void setUp() {
        service = new RoadmapJobService(jobRepository, onboardingRepository, new ObjectMapper(), publisher);
    }

    @Test
    void claimBatchMarksAttemptAndVisibleProcessingState() {
        RoadmapGenerationJob job = job(1);
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .roadmapGenerationVersion(1).build();
        when(jobRepository.lockDispatchable(eq(1), any(LocalDateTime.class))).thenReturn(List.of(job));
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.of(onboarding));

        List<RoadmapGenerationJob> claimed = service.claimBatch(1);

        assertThat(claimed).containsExactly(job);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getClaimToken()).isNotBlank();
        assertThat(job.getLockedAt()).isNotNull();
        assertThat(onboarding.getRoadmapStatus()).isEqualTo(RoadmapGenerationStatus.PROCESSING);
        assertThat(onboarding.getRoadmapGenerationAttempts()).isEqualTo(1);

        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(jobRepository).lockDispatchable(eq(1), nowCaptor.capture());
        assertThat(job.getLockedAt()).isEqualTo(nowCaptor.getValue());
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

        service.markReady(job, RoadmapResponse.builder().cefrLevel("B1").build());

        assertThat(onboarding.getRoadmapStatus()).isEqualTo(RoadmapGenerationStatus.READY);
        assertThat(onboarding.getRoadmapJson()).contains("\"cefrLevel\":\"B1\"");
    }

    @Test
    void staleClaimCannotOverwriteOnboarding() {
        RoadmapGenerationJob job = job(1);
        job.setClaimToken("stale");
        job.setAttemptCount(1);
        when(jobRepository.completeClaim(
                eq(11L), eq("stale"), eq(RoadmapGenerationStatus.READY),
                eq(null), eq(1), any(LocalDateTime.class))).thenReturn(0);

        service.markReady(job, RoadmapResponse.builder().cefrLevel("B1").build());

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

        service.markReady(job, RoadmapResponse.builder().cefrLevel("B1").build());

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
}
