package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.onboarding.RoadmapGenerationJobRepository;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RoadmapJobService {

    private static final int MAX_ATTEMPTS = 5;

    private final RoadmapGenerationJobRepository jobRepository;
    private final OnboardingRepository onboardingRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.context.ApplicationEventPublisher publisher;

    @Transactional
    public void enqueue(
            Long userId,
            int generationVersion,
            CefrLevel level,
            String goalSurveyJson) {
        if (jobRepository.existsByStudentIdAndGenerationVersion(userId, generationVersion)) return;
        LocalDateTime availableAt = LocalDateTime.now();
        jobRepository.save(RoadmapGenerationJob.builder()
                .studentId(userId)
                .generationVersion(generationVersion)
                .cefrLevel(level)
                .goalSurveyJson(goalSurveyJson)
                .status(RoadmapGenerationStatus.PENDING)
                .availableAt(availableAt)
                .build());
        publisher.publishEvent(new RoadmapJobWakeupEvent(this, availableAt));
    }

    @Transactional
    public List<RoadmapGenerationJob> claimBatch(int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<RoadmapGenerationJob> jobs = jobRepository.lockDispatchable(limit, now);
        for (RoadmapGenerationJob job : jobs) {
            int attempt = job.getAttemptCount() + 1;
            job.setStatus(RoadmapGenerationStatus.PROCESSING);
            job.setAttemptCount(attempt);
            job.setLockedAt(now);
            job.setClaimToken(UUID.randomUUID().toString());
            onboardingRepository.findByStudentId(job.getStudentId()).ifPresent(onboarding -> {
                if (Objects.equals(onboarding.getRoadmapGenerationVersion(), job.getGenerationVersion())) {
                    onboarding.setRoadmapStatus(RoadmapGenerationStatus.PROCESSING);
                    onboarding.setRoadmapGenerationAttempts(attempt);
                    onboarding.setRoadmapUpdatedAt(now);
                }
            });
        }
        return jobs;
    }

    @Transactional
    public void markReady(RoadmapGenerationJob job, RoadmapResponse roadmap) {
        final String roadmapJson;
        try {
            roadmapJson = objectMapper.writeValueAsString(roadmap);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize generated roadmap", exception);
        }

        int completed = jobRepository.completeClaim(
                job.getId(), job.getClaimToken(), RoadmapGenerationStatus.READY,
                null, job.getAttemptCount(), LocalDateTime.now());
        if (completed != 1) return;

        onboardingRepository.findByStudentId(job.getStudentId()).ifPresent(onboarding -> {
            if (Objects.equals(onboarding.getRoadmapGenerationVersion(), job.getGenerationVersion())) {
                onboarding.setRoadmapJson(roadmapJson);
                onboarding.setRoadmapStatus(RoadmapGenerationStatus.READY);
                onboarding.setRoadmapLastError(null);
                onboarding.setRoadmapUpdatedAt(LocalDateTime.now());
            }
        });
    }

    @Transactional
    public void markFailure(RoadmapGenerationJob job, Exception exception) {
        int attempts = job.getAttemptCount();
        boolean terminal = attempts >= MAX_ATTEMPTS;
        long delaySeconds = Math.min(300, 1L << Math.min(attempts, 8));
        String error = exception.getClass().getSimpleName();
        RoadmapGenerationStatus next = terminal
                ? RoadmapGenerationStatus.FAILED
                : RoadmapGenerationStatus.PENDING;
        LocalDateTime availableAt = LocalDateTime.now().plusSeconds(delaySeconds);

        int completed = jobRepository.completeClaim(
                job.getId(), job.getClaimToken(), next, error, attempts,
                availableAt);
        if (completed != 1) return;

        onboardingRepository.findByStudentId(job.getStudentId()).ifPresent(onboarding -> {
            if (Objects.equals(onboarding.getRoadmapGenerationVersion(), job.getGenerationVersion())) {
                onboarding.setRoadmapStatus(next);
                onboarding.setRoadmapGenerationAttempts(attempts);
                onboarding.setRoadmapLastError(error);
                onboarding.setRoadmapUpdatedAt(LocalDateTime.now());
            }
        });
        if (!terminal) {
            publisher.publishEvent(new RoadmapJobWakeupEvent(this, availableAt));
        }
    }

    @Transactional
    public void retry(Long userId) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(userId)
                .orElseThrow(() -> ErrorCode.PLACEMENT_TEST_NOT_FOUND.toException());
        if (onboarding.getPlacementCefrLevel() == null) {
            throw ErrorCode.PLACEMENT_TEST_INCOMPLETE.toException();
        }

        RoadmapGenerationJob job = jobRepository.findTopByStudentIdOrderByGenerationVersionDesc(userId)
                .orElseThrow(() -> ErrorCode.ROADMAP_NOT_GENERATED.toException());
        if (!job.getGenerationVersion().equals(onboarding.getRoadmapGenerationVersion())) {
            throw ErrorCode.INVALID_REQUEST.toException();
        }
        if (onboarding.getRoadmapStatus() == RoadmapGenerationStatus.READY) return;
        if (onboarding.getRoadmapStatus() == RoadmapGenerationStatus.PENDING
                || onboarding.getRoadmapStatus() == RoadmapGenerationStatus.PROCESSING) {
            throw ErrorCode.ROADMAP_GENERATING.toException();
        }

        job.setStatus(RoadmapGenerationStatus.PENDING);
        job.setAttemptCount(0);
        job.setAvailableAt(LocalDateTime.now());
        job.setLockedAt(null);
        job.setClaimToken(null);
        job.setLastError(null);
        onboarding.setRoadmapStatus(RoadmapGenerationStatus.PENDING);
        onboarding.setRoadmapGenerationAttempts(0);
        onboarding.setRoadmapLastError(null);
        onboarding.setRoadmapUpdatedAt(LocalDateTime.now());
        publisher.publishEvent(new RoadmapJobWakeupEvent(this, job.getAvailableAt()));
    }
}
