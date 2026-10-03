package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.dto.response.roadmap.RoadmapUnlockCondition;
import com.example.english_app.entity.adaptive.RoadmapModuleProgress;
import com.example.english_app.entity.enums.RoadmapItemStatus;
import com.example.english_app.entity.enums.RoadmapModuleStatus;
import com.example.english_app.entity.enums.RoadmapUnlockReason;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.adaptive.RoadmapModuleProgressRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoadmapProgressService {

    private final OnboardingRepository onboardingRepository;
    private final RoadmapContentResolver contentResolver;
    private final RoadmapModuleProgressRepository progressRepository;
    private final RoadmapProgressBatchWriter progressBatchWriter;
    private final StudentVocabularyProgressRepository vocabularyProgressRepository;
    private final SpeakingSessionRepository speakingSessionRepository;
    private final PronunciationPracticeLogRepository pronunciationPracticeLogRepository;

    @Transactional
    public RoadmapProgressResult recalculateAll(Long studentId) {
        StudentOnboarding onboarding = onboardingRepository.findByStudentId(studentId)
                .orElseThrow(() -> ErrorCode.ROADMAP_NOT_GENERATED.toException());
        return recalculateAll(onboarding);
    }

    @Transactional
    public RoadmapProgressResult recalculateAll(StudentOnboarding onboarding) {
        if (onboarding.getRoadmapJson() == null) {
            throw ErrorCode.ROADMAP_NOT_GENERATED.toException();
        }

        RoadmapResponse roadmap = contentResolver.resolve(onboarding);
        Long studentId = onboarding.getStudent().getId();
        int roadmapVersion = onboarding.getRoadmapGenerationVersion() == null
                ? 0
                : onboarding.getRoadmapGenerationVersion();
        LocalDateTime now = LocalDateTime.now();
        List<RoadmapMilestone> milestones = milestones(roadmap);
        if (milestones.isEmpty()) {
            throw new IllegalStateException("Roadmap has no milestones");
        }
        CompletionIndex completionIndex = loadCompletionIndex(studentId, milestones);
        List<RoadmapProgressBatchWriter.ProgressUpdate> updates = new ArrayList<>();

        for (RoadmapMilestone milestone : milestones) {
            List<RoadmapModule> modules = modules(milestone);
            if (modules.isEmpty()) {
                throw new IllegalStateException("Roadmap contains an empty milestone");
            }
            for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
                RoadmapModule module = modules.get(moduleIndex);
                List<Long> contentIds = requireValidModule(module);
                int totalCount = contentIds.size();
                int doneCount = completionIndex.doneCount(module.getType(), contentIds);
                RoadmapModuleStatus status = persistedStatus(doneCount, totalCount);
                updates.add(new RoadmapProgressBatchWriter.ProgressUpdate(
                        studentId,
                        roadmapVersion,
                        milestone.getWeekNumber(),
                        moduleIndex,
                        module.getModuleKey(),
                        module.getType(),
                        module.getTopicId(),
                        doneCount,
                        totalCount,
                        status,
                        status == RoadmapModuleStatus.COMPLETED ? now : null,
                        now));
            }
        }
        progressBatchWriter.upsertAll(updates);

        List<RoadmapModuleProgress> persisted = progressRepository
                .findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(
                        studentId, roadmapVersion);
        Map<String, RoadmapModuleProgress> byModuleKey = new HashMap<>();
        for (RoadmapModuleProgress progress : persisted) {
            byModuleKey.put(progress.getModuleKey(), progress);
        }
        return summarize(roadmap, roadmapVersion, byModuleKey);
    }

    private CompletionIndex loadCompletionIndex(Long studentId, List<RoadmapMilestone> milestones) {
        Set<Long> vocabularyIds = new LinkedHashSet<>();
        Set<Long> speakingIds = new LinkedHashSet<>();
        Set<Long> pronunciationIds = new LinkedHashSet<>();

        for (RoadmapMilestone milestone : milestones) {
            for (RoadmapModule module : modules(milestone)) {
                List<Long> ids = requireValidModule(module);
                switch (module.getType()) {
                    case RoadmapContentSnapshotService.VOCABULARY -> vocabularyIds.addAll(ids);
                    case RoadmapContentSnapshotService.SPEAKING -> speakingIds.addAll(ids);
                    case RoadmapContentSnapshotService.IPA_PRONUNCIATION -> pronunciationIds.addAll(ids);
                    default -> throw new IllegalStateException(
                            "Unsupported roadmap module type: " + module.getType());
                }
            }
        }

        Set<Long> practicedVocabulary = vocabularyIds.isEmpty()
                ? Set.of()
                : Set.copyOf(vocabularyProgressRepository.findPracticedVocabularyIds(
                        studentId, List.copyOf(vocabularyIds)));
        Set<Long> completedSpeaking = speakingIds.isEmpty()
                ? Set.of()
                : toLongSet(speakingSessionRepository.findCompletedScenarioIds(
                        studentId, toShortIds(List.copyOf(speakingIds))));
        Set<Long> practicedPronunciation = pronunciationIds.isEmpty()
                ? Set.of()
                : toLongSet(pronunciationPracticeLogRepository.findPracticedPhonemeIds(
                        studentId, toShortIds(List.copyOf(pronunciationIds))));
        return new CompletionIndex(practicedVocabulary, completedSpeaking, practicedPronunciation);
    }

    private RoadmapProgressResult summarize(
            RoadmapResponse roadmap,
            int roadmapVersion,
            Map<String, RoadmapModuleProgress> progressByModuleKey) {
        List<RoadmapMilestone> milestones = milestones(roadmap);
        int totalModules = 0;
        int completedModules = 0;
        int completedWeeks = 0;
        int totalItems = 0;
        int completedItems = 0;
        boolean allPriorWeeksCompleted = true;
        Integer currentWeek = null;
        RoadmapModule firstInProgress = null;
        RoadmapModule firstAvailable = null;

        for (RoadmapMilestone milestone : milestones) {
            List<RoadmapModule> weekModules = modules(milestone);
            if (weekModules.isEmpty()) {
                throw new IllegalStateException("Roadmap contains an empty milestone");
            }

            boolean weekCompleted = true;
            int weekTotalItems = 0;
            int weekCompletedItems = 0;
            for (RoadmapModule module : weekModules) {
                RoadmapModuleProgress progress = requireProgress(module, progressByModuleKey);
                totalModules++;
                totalItems += progress.getTotalCount();
                completedItems += progress.getDoneCount();
                weekTotalItems += progress.getTotalCount();
                weekCompletedItems += progress.getDoneCount();
                if (progress.getStatus() == RoadmapModuleStatus.COMPLETED) {
                    completedModules++;
                } else {
                    weekCompleted = false;
                }
            }

            boolean weekAccessible = allPriorWeeksCompleted || weekCompleted;
            RoadmapUnlockCondition unlockCondition = weekAccessible || weekCompleted
                    ? null
                    : previousWeekCondition(milestone.getWeekNumber());

            for (RoadmapModule module : weekModules) {
                RoadmapModuleProgress progress = requireProgress(module, progressByModuleKey);
                enrich(module, progress, weekAccessible, unlockCondition);
                if (weekAccessible && progress.getStatus() != RoadmapModuleStatus.COMPLETED) {
                    if (currentWeek == null) currentWeek = milestone.getWeekNumber();
                    if (firstAvailable == null) firstAvailable = module;
                    if (firstInProgress == null
                            && progress.getStatus() == RoadmapModuleStatus.IN_PROGRESS) {
                        firstInProgress = module;
                    }
                }
            }

            if (weekCompleted) completedWeeks++;
            double weekPercent = percent(weekCompletedItems, weekTotalItems);
            milestone.setStatus(apiStatus(weekCompleted, weekAccessible, weekCompletedItems > 0));
            milestone.setCompleted(weekCompleted);
            milestone.setAccessible(weekAccessible);
            milestone.setProgressPercent(roundOneDecimal(weekPercent));
            milestone.setUnlockCondition(unlockCondition);
            allPriorWeeksCompleted = allPriorWeeksCompleted && weekCompleted;
        }

        boolean roadmapCompleted = totalModules > 0 && completedModules == totalModules;
        RoadmapModule currentModule = firstInProgress != null ? firstInProgress : firstAvailable;
        if (roadmapCompleted) {
            currentWeek = null;
            currentModule = null;
        }
        double overallProgress = percent(completedItems, totalItems);
        String currentCefr = roadmap.getCurrentCefrLevel() != null
                ? roadmap.getCurrentCefrLevel()
                : roadmap.getCefrLevel();

        RoadmapProgressResponse response = RoadmapProgressResponse.builder()
                .roadmapVersion(roadmapVersion)
                .schemaVersion(roadmap.getSchemaVersion())
                .status(apiStatus(roadmapCompleted, true, completedItems > 0))
                .completed(roadmapCompleted)
                .currentCefrLevel(currentCefr)
                .targetCefrLevel(roadmap.getTargetCefrLevel())
                .cefrLevel(roadmap.getCefrLevel())
                .totalWeeks(milestones.size())
                .completedWeeks(completedWeeks)
                .currentWeek(currentWeek)
                .totalModules(totalModules)
                .completedModules(completedModules)
                .percentCompleted(roundOneDecimal(overallProgress))
                .currentModuleKey(currentModule == null ? null : currentModule.getModuleKey())
                .nextSuggestedModule(currentModule == null ? null : currentModule.getTitle())
                .milestones(milestones)
                .build();
        return new RoadmapProgressResult(roadmap, response);
    }

    private void enrich(
            RoadmapModule module,
            RoadmapModuleProgress progress,
            boolean accessible,
            RoadmapUnlockCondition unlockCondition) {
        boolean completed = progress.getStatus() == RoadmapModuleStatus.COMPLETED;
        boolean effectiveAccessible = accessible || completed;
        module.setStatus(apiStatus(completed, effectiveAccessible, progress.getDoneCount() > 0));
        module.setCompleted(completed);
        module.setAccessible(effectiveAccessible);
        module.setDoneCount(progress.getDoneCount());
        module.setTotalCount(progress.getTotalCount());
        module.setProgressPercent(roundOneDecimal(percent(
                progress.getDoneCount(), progress.getTotalCount())));
        module.setUnlockCondition(!effectiveAccessible ? unlockCondition : null);
        module.setCompletedAt(progress.getCompletedAt());
    }

    private RoadmapModuleProgress requireProgress(
            RoadmapModule module,
            Map<String, RoadmapModuleProgress> progressByModuleKey) {
        RoadmapModuleProgress progress = progressByModuleKey.get(module.getModuleKey());
        if (progress == null) {
            throw new IllegalStateException("Missing progress for module " + module.getModuleKey());
        }
        int snapshotSize = requireValidModule(module).size();
        if (progress.getTotalCount() != snapshotSize) {
            throw new IllegalStateException("Roadmap snapshot changed for module " + module.getModuleKey());
        }
        return progress;
    }

    private List<Long> requireValidModule(RoadmapModule module) {
        if (module == null || module.getModuleKey() == null || module.getModuleKey().isBlank()) {
            throw new IllegalStateException("Roadmap contains a module without moduleKey");
        }
        if (module.getModuleKey().length() > 120) {
            throw new IllegalStateException("Roadmap moduleKey exceeds 120 characters");
        }
        List<Long> contentIds = module.getContentItemIds();
        if (contentIds == null || contentIds.isEmpty()) {
            throw new IllegalStateException("Roadmap module has no content: " + module.getModuleKey());
        }
        if (contentIds.stream().anyMatch(java.util.Objects::isNull)
                || contentIds.stream().distinct().count() != contentIds.size()
                || module.getItemCount() != contentIds.size()) {
            throw new IllegalStateException(
                    "Roadmap module has an invalid content snapshot: " + module.getModuleKey());
        }
        return contentIds;
    }

    private RoadmapModuleStatus persistedStatus(int doneCount, int totalCount) {
        if (totalCount <= 0) {
            throw new IllegalStateException("Roadmap module totalCount must be positive");
        }
        if (doneCount >= totalCount) return RoadmapModuleStatus.COMPLETED;
        if (doneCount > 0) return RoadmapModuleStatus.IN_PROGRESS;
        return RoadmapModuleStatus.NOT_STARTED;
    }

    private RoadmapItemStatus apiStatus(boolean completed, boolean accessible, boolean started) {
        if (completed) return RoadmapItemStatus.COMPLETED;
        if (!accessible) return RoadmapItemStatus.LOCKED;
        if (started) return RoadmapItemStatus.IN_PROGRESS;
        return RoadmapItemStatus.AVAILABLE;
    }

    private RoadmapUnlockCondition previousWeekCondition(int weekNumber) {
        return RoadmapUnlockCondition.builder()
                .reasonCode(RoadmapUnlockReason.PREVIOUS_WEEK_REQUIRED)
                .prerequisiteWeek(Math.max(1, weekNumber - 1))
                .build();
    }

    private double percent(int doneCount, int totalCount) {
        if (totalCount <= 0) return 0.0;
        return doneCount * 100.0 / totalCount;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private List<Short> toShortIds(List<Long> ids) {
        return ids.stream().map(id -> {
            if (id < 0 || id > Short.MAX_VALUE) {
                throw new IllegalStateException("Roadmap content ID is outside SMALLINT range: " + id);
            }
            return id.shortValue();
        }).toList();
    }

    private Set<Long> toLongSet(List<Short> ids) {
        return ids.stream().map(Short::longValue).collect(Collectors.toUnmodifiableSet());
    }

    private List<RoadmapMilestone> milestones(RoadmapResponse roadmap) {
        return roadmap.getMilestones() == null ? Collections.emptyList() : roadmap.getMilestones();
    }

    private List<RoadmapModule> modules(RoadmapMilestone milestone) {
        return milestone.getModules() == null ? Collections.emptyList() : milestone.getModules();
    }

    private record CompletionIndex(
            Set<Long> vocabularyIds,
            Set<Long> speakingIds,
            Set<Long> pronunciationIds) {

        int doneCount(String moduleType, List<Long> contentIds) {
            Set<Long> completedIds = switch (moduleType) {
                case RoadmapContentSnapshotService.VOCABULARY -> vocabularyIds;
                case RoadmapContentSnapshotService.SPEAKING -> speakingIds;
                case RoadmapContentSnapshotService.IPA_PRONUNCIATION -> pronunciationIds;
                default -> throw new IllegalStateException(
                        "Unsupported roadmap module type: " + moduleType);
            };
            return Math.toIntExact(contentIds.stream()
                    .filter(completedIds::contains)
                    .distinct()
                    .count());
        }
    }

    public record RoadmapProgressResult(
            RoadmapResponse roadmap,
            RoadmapProgressResponse progress) {
    }
}
