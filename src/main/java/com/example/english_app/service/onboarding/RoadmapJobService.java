package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.adaptive.LearningEventRequest;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.enums.LearningEventSource;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.entity.enums.RoadmapGenerationStatus;
import com.example.english_app.entity.onboarding.RoadmapGenerationJob;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.onboarding.RoadmapGenerationJobRepository;
import com.example.english_app.service.adaptive.event.LearningEventOutboxService;
import com.example.english_app.service.adaptive.roadmap.RoadmapContentSnapshotService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoadmapJobService {

    private static final int MAX_ATTEMPTS = 5;

    private final RoadmapGenerationJobRepository jobRepository;
    private final OnboardingRepository onboardingRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.context.ApplicationEventPublisher publisher;
    private final LearningEventOutboxService learningEventOutboxService;

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
        LocalDateTime staleBefore = now.minusMinutes(5);
        List<RoadmapGenerationJob> jobs = jobRepository.lockDispatchable(limit, now, staleBefore);
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
        validateReadyRoadmap(roadmap);
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
            if (!Objects.equals(onboarding.getRoadmapGenerationVersion(), job.getGenerationVersion())) return;

            onboarding.setRoadmapJson(roadmapJson);
            onboarding.setRoadmapStatus(RoadmapGenerationStatus.READY);
            onboarding.setRoadmapLastError(null);
            onboarding.setRoadmapUpdatedAt(LocalDateTime.now());
            learningEventOutboxService.saveOutbox(LearningEventRequest.builder()
                    .studentId(job.getStudentId())
                    .eventType(LearningEventType.ROADMAP_GENERATED)
                    .source(LearningEventSource.ROADMAP_GENERATION)
                    .sourceReference(job.getStudentId() + ":" + job.getGenerationVersion())
                    .entityType("ROADMAP")
                    .schemaVersion((short) 1)
                    .occurredAt(LocalDateTime.now())
                    .payload(objectMapper.createObjectNode()
                            .put("roadmapVersion", job.getGenerationVersion()))
                    .build());
        });
    }

    private void validateReadyRoadmap(RoadmapResponse roadmap) {
        if (roadmap == null || roadmap.getMilestones() == null || roadmap.getMilestones().isEmpty()) {
            throw new RoadmapContentUnavailableException("Generated roadmap has no milestones");
        }
        if (roadmap.getTotalWeeks() != roadmap.getMilestones().size()) {
            throw new RoadmapContentUnavailableException("Generated roadmap has inconsistent totalWeeks");
        }

        if (roadmap.getSchemaVersion() < 2
                || roadmap.getCurrentCefrLevel() == null
                || roadmap.getTargetCefrLevel() == null) {
            throw new RoadmapContentUnavailableException("Generated roadmap has an invalid contract version");
        }
        try {
            CefrLevel.valueOf(roadmap.getCurrentCefrLevel());
            CefrLevel.valueOf(roadmap.getTargetCefrLevel());
        } catch (IllegalArgumentException exception) {
            throw new RoadmapContentUnavailableException("Generated roadmap has invalid CEFR levels");
        }

        Set<String> moduleKeys = new HashSet<>();
        for (RoadmapMilestone milestone : roadmap.getMilestones()) {
            if (milestone == null || milestone.getModules() == null || milestone.getModules().isEmpty()) {
                throw new RoadmapContentUnavailableException("Generated roadmap contains an empty milestone");
            }
            for (RoadmapModule module : milestone.getModules()) {
                if (module == null
                        || module.getModuleKey() == null || module.getModuleKey().isBlank()
                        || module.getModuleKey().length() > 120
                        || !moduleKeys.add(module.getModuleKey())
                        || !isSupportedModuleType(module.getType())
                        || module.getContentVersion() == null || module.getContentVersion().isBlank()
                        || module.getContentItemIds() == null || module.getContentItemIds().isEmpty()
                        || module.getContentItemIds().stream().distinct().count()
                                != module.getContentItemIds().size()
                        || module.getItemCount() != module.getContentItemIds().size()) {
                    throw new RoadmapContentUnavailableException(
                            "Generated roadmap contains a module without a valid content snapshot");
                }
            }
        }
    }

    private boolean isSupportedModuleType(String type) {
        return RoadmapContentSnapshotService.VOCABULARY.equals(type)
                || RoadmapContentSnapshotService.SPEAKING.equals(type)
                || RoadmapContentSnapshotService.IPA_PRONUNCIATION.equals(type);
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
