package com.example.english_app.scheduler;

import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.service.onboarding.RoadmapGenerationService;
import com.example.english_app.service.onboarding.RoadmapJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoadmapGenerationScheduler {

    private final RoadmapJobService jobService;
    private final RoadmapGenerationService generationService;

    @Value("${onboarding.roadmap.max-jobs-per-poll:10}")
    private int maxJobsPerPoll;

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

    @EventListener(com.example.english_app.service.onboarding.RoadmapJobCreatedEvent.class)
    @Async
    public void triggerOnNewJob() {
        generatePendingRoadmaps();
    }
}
