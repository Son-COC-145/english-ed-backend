package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.config.AdaptiveRecommendationProperties;
import com.example.english_app.config.AppTimeZone;
import com.example.english_app.dto.response.adaptive.TodayPlanItemResponse;
import com.example.english_app.dto.response.adaptive.TodayPlanResponse;
import com.example.english_app.entity.adaptive.LearnerProfile;
import com.example.english_app.entity.adaptive.TodayPlan;
import com.example.english_app.entity.adaptive.TodayPlanItem;
import com.example.english_app.entity.enums.RecommendationReasonCode;
import com.example.english_app.entity.enums.TodayPlanItemStatus;
import com.example.english_app.entity.onboarding.StudentOnboarding;
import com.example.english_app.exception.AppException;
import com.example.english_app.exception.ErrorCode;
import com.example.english_app.repository.adaptive.LearnerSkillStateRepository;
import com.example.english_app.repository.adaptive.StudentDailyActivityRepository;
import com.example.english_app.repository.adaptive.TodayPlanItemRepository;
import com.example.english_app.repository.adaptive.TodayPlanRepository;
import com.example.english_app.repository.gamification.StudentStatRepository;
import com.example.english_app.repository.onboarding.OnboardingRepository;
import com.example.english_app.repository.user.UserRepository;
import com.example.english_app.service.adaptive.learner.LearnerProfileService;
import com.example.english_app.service.adaptive.roadmap.RoadmapProgressService;
import com.example.english_app.service.onboarding.GoalSurveyParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TodayPlanService {

    private static final String EMPTY_REASON = "NO_ELIGIBLE_ACTIVITIES";

    private final UserRepository userRepository;
    private final OnboardingRepository onboardingRepository;
    private final LearnerProfileService learnerProfileService;
    private final LearnerSkillStateRepository skillStateRepository;
    private final RoadmapProgressService roadmapProgressService;
    private final List<TodayPlanCandidateSource> candidateSources;
    private final RecommendationScorer scorer;
    private final TodayPlanCompletionService completionService;
    private final TodayPlanRepository planRepository;
    private final TodayPlanItemRepository itemRepository;
    private final StudentStatRepository studentStatRepository;
    private final StudentDailyActivityRepository dailyActivityRepository;
    private final GoalSurveyParser goalSurveyParser;
    private final AdaptiveRecommendationProperties properties;
    private final ObjectMapper objectMapper;

    @Transactional
    public TodayPlanResponse getTodayPlan(Long studentId, Integer requestedBudgetMinutes) {
        userRepository.findByIdForUpdate(studentId)
                .orElseThrow(() -> ErrorCode.USER_NOT_FOUND.toException());

        LocalDateTime now = LocalDateTime.now(AppTimeZone.ZONE);
        LocalDate today = now.toLocalDate();
        StudentOnboarding onboarding = onboardingRepository.findByStudentIdWithUser(studentId)
                .orElseThrow(() -> ErrorCode.ROADMAP_NOT_GENERATED.toException());
        GoalSurveyParser.ParsedGoalSurvey survey = goalSurveyParser.parse(onboarding.getGoalSurveyJson());
        int budget = resolveBudget(requestedBudgetMinutes, survey.dailyStudyMinutes());

        LearnerProfile profile = learnerProfileService.ensureProfile(studentId);
        RoadmapProgressService.RoadmapProgressResult roadmap = roadmapProgressService
                .recalculateAll(onboarding);
        TodayPlanContext context = new TodayPlanContext(
                studentId,
                today,
                now,
                onboarding,
                survey,
                profile,
                skillStateRepository.findByStudentIdOrderBySkillAsc(studentId),
                roadmap.roadmap(),
                roadmap.progress());

        TodayPlan plan = planRepository.findForUpdate(studentId, today, AppTimeZone.ID).orElse(null);
        List<TodayPlanItem> items = plan == null
                ? new ArrayList<>()
                : new ArrayList<>(itemRepository.findByPlanIdOrderByPositionAsc(plan.getId()));
        if (plan != null) {
            completionService.reconcile(studentId, items, roadmap.progress(), now);
        }

        Map<String, TodayPlanItem> lockedItems = items.stream()
                .filter(item -> item.getStatus() == TodayPlanItemStatus.IN_PROGRESS
                        || item.getStatus() == TodayPlanItemStatus.COMPLETED)
                .collect(java.util.stream.Collectors.toMap(
                        TodayPlanItem::getRecommendationId,
                        item -> item,
                        (left, right) -> left));
        int lockedMinutes = lockedItems.values().stream()
                .mapToInt(TodayPlanItem::getEstimatedMinutes).sum();
        List<TodayPlanCandidate> candidates = candidateSources.stream()
                .flatMap(source -> source.collect(context).stream())
                .filter(candidate -> !lockedItems.containsKey(recommendationId(today, candidate)))
                .toList();
        List<TodayPlanCandidate> selected = applyKeepStreak(
                studentId, today, scorer.select(candidates, Math.max(0, budget - lockedMinutes)));
        String inputHash = inputHash(profile.getProfileVersion(), budget, selected);

        boolean newlyCreated = plan == null;
        boolean regenerate = newlyCreated
                || Boolean.TRUE.equals(plan.getDirty())
                || !inputHash.equals(plan.getInputHash())
                || !properties.getRulesVersion().equals(plan.getRulesVersion())
                || !Objects.equals(plan.getProfileVersion(), profile.getProfileVersion())
                || plan.getBudgetMinutes() != budget;
        if (plan == null) {
            plan = createPlan(studentId, today, now, profile.getProfileVersion(), budget, inputHash);
            plan = planRepository.saveAndFlush(plan);
        }
        if (regenerate) {
            replan(plan, items, selected, now, newlyCreated);
            plan.setProfileVersion(profile.getProfileVersion());
            plan.setRulesVersion(properties.getRulesVersion());
            plan.setBudgetMinutes(budget);
            plan.setInputHash(inputHash);
            plan.setDirty(false);
            plan.setGeneratedAt(now);
            plan.setExpiresAt(today.plusDays(1).atStartOfDay());
            plan.setUpdatedAt(now);
        }

        List<TodayPlanItem> active = items.stream()
                .filter(item -> item.getStatus() != TodayPlanItemStatus.REMOVED)
                .sorted(Comparator.comparingInt(TodayPlanItem::getPosition))
                .toList();
        updateSummary(plan, active);
        planRepository.save(plan);
        if (!items.isEmpty()) itemRepository.saveAll(items);
        return response(plan, active);
    }

    private TodayPlan createPlan(
            Long studentId,
            LocalDate date,
            LocalDateTime now,
            long profileVersion,
            int budget,
            String inputHash) {
        return TodayPlan.builder()
                .studentId(studentId)
                .planDate(date)
                .timezone(AppTimeZone.ID)
                .revision(1)
                .profileVersion(profileVersion)
                .rulesVersion(properties.getRulesVersion())
                .budgetMinutes(budget)
                .inputHash(inputHash)
                .generatedAt(now)
                .expiresAt(date.plusDays(1).atStartOfDay())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private void replan(
            TodayPlan plan,
            List<TodayPlanItem> items,
            List<TodayPlanCandidate> selected,
            LocalDateTime now,
            boolean newlyCreated) {
        int revision = newlyCreated ? 1 : plan.getRevision() + 1;
        plan.setRevision(revision);
        Map<String, TodayPlanItem> byRecommendation = new HashMap<>();
        items.forEach(item -> byRecommendation.put(item.getRecommendationId(), item));
        Map<String, TodayPlanCandidate> selectedById = new LinkedHashMap<>();
        selected.forEach(candidate -> selectedById.put(recommendationId(plan.getPlanDate(), candidate), candidate));

        int position = 0;
        for (Map.Entry<String, TodayPlanCandidate> entry : selectedById.entrySet()) {
            TodayPlanItem item = byRecommendation.get(entry.getKey());
            if (item == null) {
                item = new TodayPlanItem();
                item.setPlanId(plan.getId());
                item.setRecommendationId(entry.getKey());
                item.setCreatedAt(now);
                item.setStatus(TodayPlanItemStatus.TODO);
                item.setCompletedUnits(0);
                items.add(item);
            }
            if (item.getStatus() == TodayPlanItemStatus.TODO
                    || item.getStatus() == TodayPlanItemStatus.REMOVED) {
                copyCandidate(item, entry.getValue());
                item.setStatus(entry.getValue().completedUnits() > 0
                        ? TodayPlanItemStatus.IN_PROGRESS : TodayPlanItemStatus.TODO);
                item.setCompletedUnits(entry.getValue().completedUnits());
                item.setCompletedAt(null);
            }
            item.setPosition(position++);
            item.setLastIncludedRevision(revision);
            item.setUpdatedAt(now);
        }

        for (TodayPlanItem item : items) {
            if (selectedById.containsKey(item.getRecommendationId())) continue;
            if (item.getStatus() == TodayPlanItemStatus.TODO) {
                item.setStatus(TodayPlanItemStatus.REMOVED);
            } else if (item.getStatus() != TodayPlanItemStatus.REMOVED) {
                item.setPosition(position++);
            }
            item.setUpdatedAt(now);
        }
    }

    private void copyCandidate(TodayPlanItem item, TodayPlanCandidate candidate) {
        item.setType(candidate.type());
        item.setTitle(candidate.title());
        item.setSkill(candidate.skill());
        item.setTarget(candidate.target());
        item.setNavigation(candidate.navigation());
        item.setReasonCode(candidate.reasonCode());
        item.setReasonParams(candidate.reasonParams());
        item.setPriority(candidate.priority());
        item.setEstimatedMinutes(candidate.estimatedMinutes());
        item.setTotalUnits(candidate.totalUnits());
        item.setSourceSnapshot(candidate.sourceSnapshot());
    }

    private void updateSummary(TodayPlan plan, List<TodayPlanItem> active) {
        int estimated = active.stream().mapToInt(TodayPlanItem::getEstimatedMinutes).sum();
        int completed = active.stream().mapToInt(item -> (int) Math.round(
                item.getEstimatedMinutes() * item.getCompletedUnits().doubleValue()
                        / item.getTotalUnits())).sum();
        plan.setEstimatedMinutes(estimated);
        plan.setCompletedMinutes(Math.min(completed, estimated));
        plan.setProgressPercent(estimated == 0
                ? BigDecimal.ZERO.setScale(2)
                : BigDecimal.valueOf(completed * 100.0 / estimated)
                        .setScale(2, RoundingMode.HALF_UP));
    }

    private TodayPlanResponse response(TodayPlan plan, List<TodayPlanItem> items) {
        List<TodayPlanItemResponse> activities = items.stream().map(this::response).toList();
        String next = items.stream()
                .filter(item -> item.getStatus() != TodayPlanItemStatus.COMPLETED)
                .map(TodayPlanItem::getRecommendationId)
                .findFirst().orElse(null);
        boolean completed = !items.isEmpty() && next == null;
        return TodayPlanResponse.builder()
                .date(plan.getPlanDate())
                .timezone(plan.getTimezone())
                .revision(plan.getRevision())
                .generatedAt(plan.getGeneratedAt())
                .expiresAt(plan.getExpiresAt())
                .profileVersion(plan.getProfileVersion())
                .rulesVersion(plan.getRulesVersion())
                .budgetMinutes(plan.getBudgetMinutes())
                .estimatedMinutes(plan.getEstimatedMinutes())
                .completedMinutes(plan.getCompletedMinutes())
                .progressPercent(plan.getProgressPercent())
                .completed(completed)
                .nextRecommendationId(next)
                .emptyReason(items.isEmpty() ? EMPTY_REASON : null)
                .activities(activities)
                .build();
    }

    private TodayPlanItemResponse response(TodayPlanItem item) {
        BigDecimal progress = BigDecimal.valueOf(item.getCompletedUnits() * 100.0 / item.getTotalUnits())
                .setScale(2, RoundingMode.HALF_UP);
        return TodayPlanItemResponse.builder()
                .recommendationId(item.getRecommendationId())
                .type(item.getType())
                .title(item.getTitle())
                .skill(item.getSkill())
                .estimatedMinutes(item.getEstimatedMinutes())
                .target(item.getTarget())
                .navigation(item.getNavigation())
                .reasonCode(item.getReasonCode())
                .reasonParams(item.getReasonParams())
                .priority(item.getPriority())
                .status(item.getStatus())
                .completedUnits(item.getCompletedUnits())
                .totalUnits(item.getTotalUnits())
                .progressPercent(progress)
                .completed(item.getStatus() == TodayPlanItemStatus.COMPLETED)
                .completedAt(item.getCompletedAt())
                .build();
    }

    private int resolveBudget(Integer requested, Integer surveyBudget) {
        int budget = requested != null
                ? requested
                : surveyBudget != null ? surveyBudget : properties.getDefaultBudgetMinutes();
        if (budget < 5 || budget > 120) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "budgetMinutes must be between 5 and 120");
        }
        return budget;
    }

    private List<TodayPlanCandidate> applyKeepStreak(
            Long studentId,
            LocalDate today,
            List<TodayPlanCandidate> selected) {
        short streak = studentStatRepository.findByStudentId(studentId)
                .map(stat -> stat.getCurrentStreak() == null ? (short) 0 : stat.getCurrentStreak())
                .orElse((short) 0);
        boolean studiedToday = dailyActivityRepository.findByStudentIdAndActivityDate(studentId, today)
                .map(activity -> activity.getActivityCount() > 0)
                .orElse(false)
                || completionService.hasLearningActivitySince(studentId, today.atStartOfDay());
        if (streak < 3 || studiedToday || selected.isEmpty()) return selected;

        TodayPlanCandidate shortest = selected.stream()
                .filter(candidate -> candidate.priority() == null)
                .min(Comparator.comparingInt(TodayPlanCandidate::estimatedMinutes))
                .orElse(null);
        if (shortest == null) return selected;
        var reason = objectMapper.createObjectNode().put("streak", streak);
        TodayPlanCandidate replacement = new TodayPlanCandidate(
                shortest.type(), shortest.title(), shortest.skill(), shortest.target(),
                shortest.navigation(), RecommendationReasonCode.KEEP_STREAK, reason,
                shortest.priority(), shortest.estimatedMinutes(), shortest.completedUnits(),
                shortest.totalUnits(), shortest.sourceSnapshot(), shortest.dueUrgency(),
                shortest.weakness(), shortest.roadmapRelevance(), shortest.goalRelevance(),
                shortest.freshness(), shortest.deadline());
        return selected.stream().map(candidate -> candidate == shortest ? replacement : candidate).toList();
    }

    private String inputHash(long profileVersion, int budget, List<TodayPlanCandidate> selected) {
        StringBuilder input = new StringBuilder()
                .append(profileVersion).append('|')
                .append(properties.getRulesVersion()).append('|')
                .append(budget);
        selected.forEach(candidate -> input.append('|').append(candidate.type())
                .append(':').append(json(candidate.target()))
                .append(':').append(candidate.reasonCode())
                .append(':').append(json(candidate.reasonParams()))
                .append(':').append(json(candidate.sourceSnapshot()))
                .append(':').append(candidate.estimatedMinutes())
                .append(':').append(candidate.completedUnits())
                .append(':').append(candidate.totalUnits()));
        return sha256(input.toString());
    }

    private String recommendationId(LocalDate date, TodayPlanCandidate candidate) {
        return sha256(date + "|" + candidate.type() + "|" + json(candidate.target()));
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Cannot serialize Today Plan input", exception);
        }
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
