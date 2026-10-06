package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.config.AdaptiveRecommendationProperties;
import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.RecommendationPriority;
import com.example.english_app.entity.enums.TodayPlanItemType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RecommendationScorer {

    private final AdaptiveRecommendationProperties properties;

    public List<TodayPlanCandidate> select(List<TodayPlanCandidate> candidates, int budgetMinutes) {
        List<TodayPlanCandidate> distinct = candidates.stream()
                .filter(this::valid)
                .collect(java.util.stream.Collectors.toMap(
                        this::identity,
                        candidate -> candidate,
                        this::prefer,
                        java.util.LinkedHashMap::new))
                .values().stream().toList();

        Comparator<TodayPlanCandidate> urgentOrder = Comparator
                .comparing((TodayPlanCandidate candidate) -> candidate.priority() == RecommendationPriority.P0 ? 0 : 1)
                .thenComparing(TodayPlanCandidate::deadline,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(this::identity);
        Comparator<TodayPlanCandidate> scoreOrder = Comparator
                .comparingDouble(this::score).reversed()
                .thenComparing(this::identity);

        List<TodayPlanCandidate> selected = new ArrayList<>();
        distinct.stream().filter(candidate -> candidate.priority() != null)
                .sorted(urgentOrder).forEach(selected::add);

        List<TodayPlanCandidate> regular = distinct.stream()
                .filter(candidate -> candidate.priority() == null)
                .sorted(scoreOrder)
                .toList();
        SelectionState state = SelectionState.from(selected);
        int remaining = budgetMinutes - selected.stream()
                .mapToInt(TodayPlanCandidate::estimatedMinutes).sum();

        if (selected.isEmpty()) {
            for (TodayPlanCandidate candidate : regular) {
                if (canAdd(candidate, remaining, state)) {
                    selected.add(candidate);
                    state.add(candidate);
                    remaining -= candidate.estimatedMinutes();
                    break;
                }
            }
        }
        if (state.skills.size() < 2) {
            for (TodayPlanCandidate candidate : regular) {
                if (!selected.contains(candidate) && candidate.skill() != null
                        && !state.skills.contains(candidate.skill())
                        && canAdd(candidate, remaining, state)) {
                    selected.add(candidate);
                    state.add(candidate);
                    remaining -= candidate.estimatedMinutes();
                    break;
                }
            }
        }
        for (TodayPlanCandidate candidate : regular) {
            if (!selected.contains(candidate) && canAdd(candidate, remaining, state)) {
                selected.add(candidate);
                state.add(candidate);
                remaining -= candidate.estimatedMinutes();
            }
        }
        return List.copyOf(selected);
    }

    public double score(TodayPlanCandidate candidate) {
        var weights = properties.getWeights();
        return candidate.dueUrgency() * weights.getDueUrgency()
                + candidate.weakness() * weights.getWeakness()
                + candidate.roadmapRelevance() * weights.getRoadmap()
                + candidate.goalRelevance() * weights.getGoal()
                + candidate.freshness() * weights.getFreshness();
    }

    private boolean valid(TodayPlanCandidate candidate) {
        return candidate != null && candidate.type() != null && candidate.target() != null
                && candidate.navigation() != null && candidate.reasonCode() != null
                && candidate.estimatedMinutes() > 0 && candidate.totalUnits() > 0
                && candidate.completedUnits() < candidate.totalUnits();
    }

    private boolean canAdd(TodayPlanCandidate candidate, int remaining, SelectionState state) {
        if (candidate.estimatedMinutes() > remaining) return false;
        int typeLimit = candidate.type() == TodayPlanItemType.PRONUNCIATION
                || candidate.type() == TodayPlanItemType.ASSIGNMENT ? 2 : 1;
        if (state.typeCounts.getOrDefault(candidate.type(), 0) >= typeLimit) return false;
        return candidate.skill() == null || state.skillCounts.getOrDefault(candidate.skill(), 0) < 2;
    }

    private TodayPlanCandidate prefer(TodayPlanCandidate left, TodayPlanCandidate right) {
        if (left.priority() != null && right.priority() == null) return left;
        if (right.priority() != null && left.priority() == null) return right;
        return score(left) >= score(right) ? left : right;
    }

    private String identity(TodayPlanCandidate candidate) {
        return candidate.type().name() + ":" + candidate.target().toString();
    }

    private static final class SelectionState {
        private final EnumMap<TodayPlanItemType, Integer> typeCounts = new EnumMap<>(TodayPlanItemType.class);
        private final EnumMap<LearnerSkill, Integer> skillCounts = new EnumMap<>(LearnerSkill.class);
        private final Set<LearnerSkill> skills = new HashSet<>();

        static SelectionState from(List<TodayPlanCandidate> candidates) {
            SelectionState state = new SelectionState();
            candidates.forEach(state::add);
            return state;
        }

        void add(TodayPlanCandidate candidate) {
            typeCounts.merge(candidate.type(), 1, Integer::sum);
            if (candidate.skill() != null) {
                skillCounts.merge(candidate.skill(), 1, Integer::sum);
                skills.add(candidate.skill());
            }
        }
    }
}
