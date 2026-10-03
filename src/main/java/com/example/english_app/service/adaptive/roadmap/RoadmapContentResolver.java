package com.example.english_app.service.adaptive.roadmap;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapResponse;
import com.example.english_app.entity.enums.CefrLevel;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoadmapContentResolver {

    private final ObjectMapper objectMapper;
    private final OnboardingRepository onboardingRepository;
    private final RoadmapContentSnapshotService contentSnapshotService;

    /**
     * Reads the stored roadmap and upgrades legacy modules exactly once. The upgrade is
     * deterministic, so concurrent first reads can safely converge to the same JSON.
     */
    @Transactional
    public RoadmapResponse resolve(StudentOnboarding onboarding) {
        RoadmapResponse roadmap = read(onboarding.getRoadmapJson());
        List<RoadmapMilestone> milestones = mutableMilestones(roadmap);
        CefrLevel level = roadmapLevel(roadmap, onboarding);
        boolean changed = false;

        if (roadmap.getCurrentCefrLevel() == null) {
            roadmap.setCurrentCefrLevel(level.name());
            changed = true;
        }
        if (roadmap.getTargetCefrLevel() == null) {
            roadmap.setTargetCefrLevel(nextLevel(level).name());
            changed = true;
        }
        if (roadmap.getCefrLevel() == null) {
            roadmap.setCefrLevel(level.name());
            changed = true;
        }

        if (removeLegacyIpaModules(milestones)) {
            distributeIpaModules(milestones, contentSnapshotService.ipaModuleGroups(level));
            changed = true;
        }

        Set<String> moduleKeys = new HashSet<>();
        for (int weekIndex = 0; weekIndex < milestones.size(); weekIndex++) {
            RoadmapMilestone milestone = milestones.get(weekIndex);
            if (milestone.getWeekNumber() <= 0) {
                milestone.setWeekNumber(weekIndex + 1);
                changed = true;
            }
            List<RoadmapModule> modules = mutableModules(milestone);
            clearApiProgress(milestone);
            for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
                RoadmapModule module = modules.get(moduleIndex);
                if (!contentSnapshotService.isSnapshotComplete(module)) {
                    contentSnapshotService.resolveLegacyModule(
                            module, level, milestone.getWeekNumber(), moduleIndex);
                    changed = true;
                }
                if (!moduleKeys.add(module.getModuleKey())) {
                    throw new IllegalStateException("Duplicate roadmap module key: " + module.getModuleKey());
                }
                clearApiProgress(module);
            }
        }

        if (roadmap.getTotalWeeks() != milestones.size()) {
            roadmap.setTotalWeeks(milestones.size());
            changed = true;
        }
        if (changed) {
            try {
                onboarding.setRoadmapJson(objectMapper.writeValueAsString(roadmap));
                onboardingRepository.save(onboarding);
            } catch (JsonProcessingException exception) {
                throw new IllegalStateException("Cannot persist upgraded roadmap", exception);
            }
        }
        return roadmap;
    }

    private RoadmapResponse read(String roadmapJson) {
        if (roadmapJson == null || roadmapJson.isBlank()) {
            throw new IllegalStateException("Roadmap JSON is missing");
        }
        try {
            return objectMapper.readValue(roadmapJson, RoadmapResponse.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot parse roadmap JSON", exception);
        }
    }

    private List<RoadmapMilestone> mutableMilestones(RoadmapResponse roadmap) {
        List<RoadmapMilestone> milestones = roadmap.getMilestones() == null
                ? new ArrayList<>()
                : new ArrayList<>(roadmap.getMilestones());
        roadmap.setMilestones(milestones);
        return milestones;
    }

    private List<RoadmapModule> mutableModules(RoadmapMilestone milestone) {
        List<RoadmapModule> modules = milestone.getModules() == null
                ? new ArrayList<>()
                : new ArrayList<>(milestone.getModules());
        milestone.setModules(modules);
        return modules;
    }

    private boolean removeLegacyIpaModules(List<RoadmapMilestone> milestones) {
        boolean removed = false;
        for (RoadmapMilestone milestone : milestones) {
            List<RoadmapModule> modules = mutableModules(milestone);
            removed |= modules.removeIf(module ->
                    RoadmapContentSnapshotService.IPA_PRONUNCIATION.equals(module.getType())
                            && !contentSnapshotService.isSnapshotComplete(module));
        }
        return removed;
    }

    private void distributeIpaModules(
            List<RoadmapMilestone> milestones,
            List<RoadmapContentSnapshotService.IpaModuleGroup> groups) {
        if (milestones.isEmpty()) return;
        int lastWeekIndex = milestones.size() - 1;
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            RoadmapMilestone milestone = milestones.get(Math.min(groupIndex, lastWeekIndex));
            List<RoadmapModule> modules = mutableModules(milestone);
            modules.addAll(groups.get(groupIndex).modules());
        }
    }

    private CefrLevel roadmapLevel(RoadmapResponse roadmap, StudentOnboarding onboarding) {
        if (roadmap.getCefrLevel() != null) {
            try {
                return CefrLevel.valueOf(roadmap.getCefrLevel());
            } catch (IllegalArgumentException ignored) {
                // Fall through to the persisted placement level.
            }
        }
        return onboarding.getPlacementCefrLevel() != null
                ? onboarding.getPlacementCefrLevel()
                : CefrLevel.A1;
    }

    private void clearApiProgress(RoadmapModule module) {
        module.setStatus(null);
        module.setCompleted(null);
        module.setAccessible(null);
        module.setDoneCount(null);
        module.setTotalCount(null);
        module.setProgressPercent(null);
        module.setUnlockCondition(null);
        module.setCompletedAt(null);
    }

    private void clearApiProgress(RoadmapMilestone milestone) {
        milestone.setStatus(null);
        milestone.setCompleted(null);
        milestone.setAccessible(null);
        milestone.setProgressPercent(null);
        milestone.setUnlockCondition(null);
    }

    private CefrLevel nextLevel(CefrLevel level) {
        return switch (level) {
            case A1 -> CefrLevel.A2;
            case A2 -> CefrLevel.B1;
            case B1 -> CefrLevel.B2;
            case B2 -> CefrLevel.C1;
            case C1, C2 -> CefrLevel.C2;
        };
    }
}
