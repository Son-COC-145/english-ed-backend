package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.dto.request.adaptive.LearningEventRequest;
import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.entity.adaptive.LearningEvent;
import com.example.english_app.entity.enums.LearningEventSource;
import com.example.english_app.entity.enums.LearningEventType;
import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.service.adaptive.event.LearningEventConsumer;
import com.example.english_app.service.adaptive.event.LearningEventConsumerStage;
import com.example.english_app.service.adaptive.event.LearningEventOutboxService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RoadmapProgressConsumer implements LearningEventConsumer {

    private static final Set<LearningEventType> SUPPORTED_EVENTS = EnumSet.of(
            LearningEventType.ROADMAP_GENERATED,
            LearningEventType.VOCAB_REVIEWED,
            LearningEventType.VOCAB_ROUND_COMPLETED,
            LearningEventType.PRONUNCIATION_PRACTICED,
            LearningEventType.SPEAKING_SESSION_EVALUATED);

    private final OnboardingRepository onboardingRepository;
    private final RoadmapProgressService progressService;
    private final LearningEventOutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Override
    public LearningEventConsumerStage stage() {
        return LearningEventConsumerStage.ROADMAP_PROGRESS;
    }

    @Override
    public boolean supports(LearningEventType eventType) {
        return SUPPORTED_EVENTS.contains(eventType);
    }

    @Override
    public void apply(LearningEvent event) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(event.getStudentId())
                .filter(candidate -> candidate.getRoadmapJson() != null
                        && !candidate.getRoadmapJson().isBlank())
                .orElse(null);
        if (onboarding == null) return;

        RoadmapProgressService.RoadmapProgressResult result = progressService.recalculateAll(onboarding);
        int roadmapVersion = result.progress().getRoadmapVersion();
        List<RoadmapMilestone> milestones = result.progress().getMilestones();

        for (RoadmapMilestone milestone : milestones) {
            for (RoadmapModule module : milestone.getModules()) {
                if (module.getStatus() == RoadmapItemStatus.COMPLETED) {
                    publishModuleCompleted(event, roadmapVersion, milestone.getWeekNumber(), module);
                }
            }
            if (milestone.getStatus() == RoadmapItemStatus.COMPLETED) {
                publishWeekCompleted(event, roadmapVersion, milestone.getWeekNumber());
            }
        }
        if (result.progress().isCompleted()) {
            publishRoadmapCompleted(event, roadmapVersion);
        }
    }

    private void publishModuleCompleted(
            LearningEvent cause,
            int roadmapVersion,
            int weekNumber,
            RoadmapModule module) {
        outboxService.saveOutbox(baseRequest(
                cause,
                LearningEventType.ROADMAP_MODULE_COMPLETED,
                cause.getStudentId() + ":" + roadmapVersion + ":MODULE:"
                        + stableHash(module.getModuleKey()),
                "ROADMAP_MODULE")
                .payload(objectMapper.createObjectNode()
                        .put("roadmapVersion", roadmapVersion)
                        .put("weekNumber", weekNumber)
                        .put("moduleKey", module.getModuleKey()))
                .build());
    }

    private void publishWeekCompleted(LearningEvent cause, int roadmapVersion, int weekNumber) {
        outboxService.saveOutbox(baseRequest(
                cause,
                LearningEventType.ROADMAP_WEEK_COMPLETED,
                cause.getStudentId() + ":" + roadmapVersion + ":WEEK:" + weekNumber,
                "ROADMAP_WEEK")
                .payload(objectMapper.createObjectNode()
                        .put("roadmapVersion", roadmapVersion)
                        .put("weekNumber", weekNumber))
                .build());
    }

    private void publishRoadmapCompleted(LearningEvent cause, int roadmapVersion) {
        outboxService.saveOutbox(baseRequest(
                cause,
                LearningEventType.ROADMAP_COMPLETED,
                cause.getStudentId() + ":" + roadmapVersion + ":ALL",
                "ROADMAP")
                .payload(objectMapper.createObjectNode()
                        .put("roadmapVersion", roadmapVersion))
                .build());
    }

    private LearningEventRequest.LearningEventRequestBuilder baseRequest(
            LearningEvent cause,
            LearningEventType eventType,
            String sourceReference,
            String entityType) {
        return LearningEventRequest.builder()
                .studentId(cause.getStudentId())
                .eventType(eventType)
                .source(LearningEventSource.ROADMAP)
                .sourceReference(sourceReference)
                .entityType(entityType)
                .schemaVersion((short) 1)
                .occurredAt(LocalDateTime.now())
                .causationEventId(cause.getEventId());
    }

    private String stableHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
