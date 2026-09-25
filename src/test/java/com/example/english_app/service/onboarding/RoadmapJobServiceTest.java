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
import org.mockito.junit.jupiter.MockitoExtension;

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
    private RoadmapJobService service;

    @BeforeEach
    void setUp() {
        service = new RoadmapJobService(jobRepository, onboardingRepository, new ObjectMapper());
    }

    @Test
    void claimBatchMarksAttemptAndVisibleProcessingState() {
        RoadmapGenerationJob job = job(1);
        StudentOnboarding onboarding = StudentOnboarding.builder()
                .roadmapGenerationVersion(1).build();
        when(jobRepository.lockDispatchable(1)).thenReturn(List.of(job));
        when(onboardingRepository.findByStudentId(7L)).thenReturn(Optional.of(onboarding));

        List<RoadmapGenerationJob> claimed = service.claimBatch(1);

        assertThat(claimed).containsExactly(job);
        assertThat(job.getAttemptCount()).isEqualTo(1);
        assertThat(job.getClaimToken()).isNotBlank();
        assertThat(onboarding.getRoadmapStatus()).isEqualTo(RoadmapGenerationStatus.PROCESSING);
        assertThat(onboarding.getRoadmapGenerationAttempts()).isEqualTo(1);
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
