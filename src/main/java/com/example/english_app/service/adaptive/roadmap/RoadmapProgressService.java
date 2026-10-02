package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.adaptive.RoadmapModuleProgress;
import com.example.english_app.entity.enums.RoadmapModuleStatus;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RoadmapProgressService {

    private final OnboardingRepository onboardingRepository;
    private final RoadmapContentResolver contentResolver;
    private final RoadmapModuleProgressRepository progressRepository;
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

        List<RoadmapMilestone> milestones = roadmap.getMilestones() == null
                ? Collections.emptyList()
                : roadmap.getMilestones();
        for (int weekIndex = 0; weekIndex < milestones.size(); weekIndex++) {
            RoadmapMilestone milestone = milestones.get(weekIndex);
            List<RoadmapModule> modules = milestone.getModules() == null
                    ? Collections.emptyList()
                    : milestone.getModules();
            for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
                RoadmapModule module = modules.get(moduleIndex);
                List<Long> contentIds = module.getContentItemIds() == null
                        ? List.of()
                        : module.getContentItemIds();
                int totalCount = contentIds.size();
                int doneCount = Math.min(totalCount, countDone(studentId, module.getType(), contentIds));
                RoadmapModuleStatus status = status(doneCount, totalCount);
                int affected = progressRepository.upsert(
                        studentId,
                        roadmapVersion,
                        milestone.getWeekNumber(),
                        moduleIndex,
                        module.getModuleKey(),
                        module.getType(),
                        module.getTopicId(),
                        doneCount,
                        totalCount,
                        status.name(),
                        status == RoadmapModuleStatus.COMPLETED ? now : null,
                        now);
                if (affected != 1) {
                    throw new IllegalStateException("Roadmap progress upsert affected " + affected + " rows");
                }
            }
        }

        List<RoadmapModuleProgress> persisted =
                progressRepository.findByStudentIdAndRoadmapVersionOrderByWeekNumberAscModuleIndexAsc(
                        studentId, roadmapVersion);
        Map<String, RoadmapModuleProgress> byModuleKey = new HashMap<>();
        for (RoadmapModuleProgress progress : persisted) {
            byModuleKey.put(progress.getModuleKey(), progress);
        }

        return summarize(roadmap, roadmapVersion, byModuleKey);
    }

    private RoadmapProgressResult summarize(
            RoadmapResponse roadmap,
            int roadmapVersion,
            Map<String, RoadmapModuleProgress> progressByModuleKey) {
        List<RoadmapMilestone> milestones = roadmap.getMilestones() == null
                ? Collections.emptyList()
                : roadmap.getMilestones();
        int totalModules = 0;
        int completedModules = 0;
        int completedWeeks = 0;
        Integer currentWeek = null;
        String nextSuggestedModule = null;

        for (RoadmapMilestone milestone : milestones) {
            List<RoadmapModule> modules = milestone.getModules() == null
                    ? Collections.emptyList()
                    : milestone.getModules();
            boolean weekCompleted = !modules.isEmpty();
            for (RoadmapModule module : modules) {
                totalModules++;
                RoadmapModuleProgress progress = progressByModuleKey.get(module.getModuleKey());
                if (progress == null) {
                    throw new IllegalStateException("Missing progress for module " + module.getModuleKey());
                }
                int snapshotSize = module.getContentItemIds() == null
                        ? 0
                        : module.getContentItemIds().size();
                if (progress.getTotalCount() != snapshotSize) {
                    throw new IllegalStateException(
                            "Roadmap snapshot changed for module " + module.getModuleKey());
                }
                enrich(module, progress);
                if (progress.getStatus() == RoadmapModuleStatus.COMPLETED) {
                    completedModules++;
                } else {
                    weekCompleted = false;
                    if (currentWeek == null) currentWeek = milestone.getWeekNumber();
                    if (nextSuggestedModule == null) nextSuggestedModule = module.getTitle();
                }
            }
            if (weekCompleted) completedWeeks++;
        }

        if (currentWeek == null && !milestones.isEmpty()) {
            currentWeek = milestones.getLast().getWeekNumber();
        }
        double percent = totalModules == 0
                ? 0.0
                : roundOneDecimal(completedModules * 100.0 / totalModules);

        RoadmapProgressResponse response = RoadmapProgressResponse.builder()
                .roadmapVersion(roadmapVersion)
                .cefrLevel(roadmap.getCefrLevel())
                .totalWeeks(milestones.size())
                .completedWeeks(completedWeeks)
                .currentWeek(currentWeek == null ? 0 : currentWeek)
                .totalModules(totalModules)
                .completedModules(completedModules)
                .percentCompleted(percent)
                .nextSuggestedModule(nextSuggestedModule)
                .milestones(milestones)
                .build();
        return new RoadmapProgressResult(roadmap, response);
    }

    private int countDone(Long studentId, String moduleType, List<Long> contentIds) {
        if (contentIds.isEmpty()) return 0;
        long done = switch (moduleType) {
            case RoadmapContentSnapshotService.VOCABULARY ->
                    vocabularyProgressRepository.countPracticedVocabularyIds(studentId, contentIds);
            case RoadmapContentSnapshotService.SPEAKING ->
                    speakingSessionRepository.countCompletedScenarioIds(studentId, toShortIds(contentIds));
            case RoadmapContentSnapshotService.IPA_PRONUNCIATION ->
                    pronunciationPracticeLogRepository.countPracticedPhonemeIds(studentId, toShortIds(contentIds));
            default -> 0L;
        };
        return Math.toIntExact(done);
    }

    private List<Short> toShortIds(List<Long> ids) {
        return ids.stream().map(id -> {
            if (id < 0 || id > Short.MAX_VALUE) {
                throw new IllegalStateException("Roadmap content ID is outside SMALLINT range: " + id);
            }
            return id.shortValue();
        }).toList();
    }

    private RoadmapModuleStatus status(int doneCount, int totalCount) {
        if (totalCount == 0 || doneCount == totalCount) return RoadmapModuleStatus.COMPLETED;
        if (doneCount > 0) return RoadmapModuleStatus.IN_PROGRESS;
        return RoadmapModuleStatus.NOT_STARTED;
    }

    private void enrich(RoadmapModule module, RoadmapModuleProgress progress) {
        module.setStatus(progress.getStatus().name());
        module.setDoneCount(progress.getDoneCount());
        module.setTotalCount(progress.getTotalCount());
        double percent = progress.getTotalCount() == 0
                ? 100.0
                : progress.getDoneCount() * 100.0 / progress.getTotalCount();
        module.setProgressPercent(roundOneDecimal(percent));
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    public record RoadmapProgressResult(
            RoadmapResponse roadmap,
            RoadmapProgressResponse progress) {
    }
}
