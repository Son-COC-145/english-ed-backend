package com.example.english_app.scheduler;

import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import com.example.english_app.service.onboarding.RoadmapGenerationService;
import com.example.english_app.service.onboarding.RoadmapJobService;
import com.example.english_app.service.onboarding.RoadmapJobWakeupEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoadmapGenerationSchedulerTest {

    @Mock private RoadmapJobService jobService;
    @Mock private RoadmapGenerationService generationService;
    @Mock private ScheduledExecutorService wakeupExecutor;

    private RoadmapGenerationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new RoadmapGenerationScheduler(jobService, generationService, wakeupExecutor, 10);
    }

    @Test
    void availableJobRunsImmediatelyWithoutWaitingForDurablePoll() {
        RoadmapGenerationJob job = job();
        RoadmapResponse roadmap = RoadmapResponse.builder().cefrLevel("B1").build();
        when(jobService.claimBatch(1)).thenReturn(List.of(job), List.of());
        when(generationService.generateRoadmap(CefrLevel.B1, "{}")).thenReturn(roadmap);

        scheduler.triggerOnAvailableJob(
                new RoadmapJobWakeupEvent(this, LocalDateTime.now().minusSeconds(1)));

        verify(jobService).markReady(job, roadmap);
        verifyNoInteractions(wakeupExecutor);
    }

    @Test
    void futureRetryIsScheduledInMemoryInsteadOfPollingDatabase() {
        LocalDateTime availableAt = LocalDateTime.now().plusSeconds(30);

        scheduler.triggerOnAvailableJob(new RoadmapJobWakeupEvent(this, availableAt));

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Long> delayCaptor = ArgumentCaptor.forClass(Long.class);
        verify(wakeupExecutor).schedule(taskCaptor.capture(), delayCaptor.capture(), eq(TimeUnit.MILLISECONDS));
        assertThat(delayCaptor.getValue()).isBetween(28_000L, 30_000L);
        verifyNoInteractions(jobService);

        when(jobService.claimBatch(1)).thenReturn(List.of());
        taskCaptor.getValue().run();
        verify(jobService).claimBatch(1);
    }

    @Test
    void failedGenerationReturnsJobToDurableRetryFlow() {
        RoadmapGenerationJob job = job();
        IllegalStateException failure = new IllegalStateException("database unavailable");
        when(jobService.claimBatch(1)).thenReturn(List.of(job), List.of());
        when(generationService.generateRoadmap(CefrLevel.B1, "{}")).thenThrow(failure);

        scheduler.generatePendingRoadmaps();

        verify(jobService).markFailure(job, failure);
        verify(jobService, never()).markReady(eq(job), any(RoadmapResponse.class));
    }

    private RoadmapGenerationJob job() {
        return RoadmapGenerationJob.builder()
                .id(11L)
                .studentId(7L)
                .generationVersion(1)
                .cefrLevel(CefrLevel.B1)
                .goalSurveyJson("{}")
                .build();
    }
}
