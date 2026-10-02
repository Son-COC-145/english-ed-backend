package com.example.english_app.scheduler;

import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.service.onboarding.RoadmapGenerationService;
import com.example.english_app.service.onboarding.RoadmapJobService;
import com.example.english_app.service.onboarding.RoadmapJobWakeupEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RoadmapGenerationScheduler {

    private final RoadmapJobService jobService;
    private final RoadmapGenerationService generationService;
    private final ScheduledExecutorService wakeupExecutor;
    private final int maxJobsPerPoll;

    public RoadmapGenerationScheduler(
            RoadmapJobService jobService,
            RoadmapGenerationService generationService,
            @Qualifier("roadmapWakeupExecutor") ScheduledExecutorService wakeupExecutor,
            @Value("${onboarding.roadmap.max-jobs-per-poll:10}") int maxJobsPerPoll) {
        this.jobService = jobService;
        this.generationService = generationService;
        this.wakeupExecutor = wakeupExecutor;
        this.maxJobsPerPoll = maxJobsPerPoll;
    }

    @Scheduled(fixedDelayString = "${onboarding.roadmap.poll-ms:1800000}")
    public void generatePendingRoadmaps() {
        for (int index = 0; index < maxJobsPerPoll; index++) {
            List<RoadmapGenerationJob> claimed = jobService.claimBatch(1);
            if (claimed.isEmpty()) return;
            RoadmapGenerationJob job = claimed.getFirst();
            try {
                RoadmapResponse roadmap = generationService.generateRoadmap(
                        job.getCefrLevel(), job.getGoalSurveyJson());
                jobService.markReady(job, roadmap);
            } catch (Exception exception) {
                log.warn("Roadmap generation failed: jobId={}, userId={}, version={}",
                        job.getId(), job.getStudentId(), job.getGenerationVersion(), exception);
                jobService.markFailure(job, exception);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void triggerOnAvailableJob(RoadmapJobWakeupEvent event) {
        long delayMs = Math.max(0L,
                Duration.between(LocalDateTime.now(), event.getAvailableAt()).toMillis());
        if (delayMs == 0L) {
            runWorkerSafely();
            return;
        }

        try {
            wakeupExecutor.schedule(this::runWorkerSafely, delayMs, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException exception) {
            // Expected only while the application is shutting down. The durable sweep will recover the job.
            log.debug("Roadmap wake-up was rejected during shutdown; durable polling will recover it");
        }
    }

    private void runWorkerSafely() {
        try {
            generatePendingRoadmaps();
        } catch (RuntimeException exception) {
            // Do not let an infrastructure error terminate the delayed executor thread.
            log.error("Cannot dispatch pending roadmap jobs; durable polling will retry", exception);
        }
    }
}
