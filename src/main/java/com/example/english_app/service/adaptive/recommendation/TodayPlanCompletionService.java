package com.example.english_app.service.adaptive.recommendation;

import com.example.english_app.dto.response.roadmap.RoadmapMilestone;
import com.example.english_app.dto.response.roadmap.RoadmapModule;
import com.example.english_app.dto.response.roadmap.RoadmapProgressResponse;
import com.example.english_app.entity.adaptive.TodayPlanItem;
import com.example.english_app.entity.enums.TodayPlanItemStatus;
import com.example.english_app.repository.classroom.AssignmentSubmissionRepository;
import com.example.english_app.repository.ipa.PronunciationPracticeLogRepository;
import com.example.english_app.repository.speaking.SpeakingSessionRepository;
import com.example.english_app.repository.vocabulary.StudentVocabularyProgressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodayPlanCompletionService {

    private final StudentVocabularyProgressRepository vocabularyProgressRepository;
    private final PronunciationPracticeLogRepository pronunciationPracticeRepository;
    private final SpeakingSessionRepository speakingSessionRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;

    public boolean reconcile(
            Long studentId,
            List<TodayPlanItem> items,
            RoadmapProgressResponse roadmapProgress,
            LocalDateTime now) {
        boolean changed = false;
        for (TodayPlanItem item : items) {
            if (item.getStatus() == TodayPlanItemStatus.REMOVED) continue;
            int completedUnits = completedUnits(studentId, item, roadmapProgress);
            completedUnits = Math.max(0, Math.min(completedUnits, item.getTotalUnits()));
            TodayPlanItemStatus status = status(completedUnits, item.getTotalUnits());
            if (item.getCompletedUnits() != completedUnits || item.getStatus() != status) {
                item.setCompletedUnits(completedUnits);
                item.setStatus(status);
                item.setCompletedAt(status == TodayPlanItemStatus.COMPLETED
                        ? firstNonNull(item.getCompletedAt(), now)
                        : null);
                item.setUpdatedAt(now);
                changed = true;
            }
        }
        return changed;
    }

    public boolean hasLearningActivitySince(Long studentId, LocalDateTime since) {
        return vocabularyProgressRepository
                .countByStudentIdAndLastPracticedAtGreaterThanEqual(studentId, since) > 0
                || pronunciationPracticeRepository
                .countByStudentIdAndPracticedAtGreaterThanEqual(studentId, since) > 0
                || speakingSessionRepository
                .countByStudentIdAndEndedAtGreaterThanEqual(studentId, since) > 0;
    }

    private int completedUnits(
            Long studentId,
            TodayPlanItem item,
            RoadmapProgressResponse roadmapProgress) {
        LocalDateTime since = firstNonNull(item.getCreatedAt(), LocalDateTime.of(1970, 1, 1, 0, 0));
        return switch (item.getType()) {
            case VOCABULARY_REVIEW, VOCABULARY_TOPIC -> vocabularyCompletedUnits(
                    studentId, item, since);
            case ROADMAP_MODULE -> roadmapCompletedUnits(item, roadmapProgress);
            case PRONUNCIATION -> Math.toIntExact(Math.min(1,
                    pronunciationPracticeRepository.countPhonemePracticesSince(
                            studentId, shortValue(item.getTarget(), "phonemeId"), since)));
            case SPEAKING -> Math.toIntExact(Math.min(1,
                    speakingSessionRepository.countCompletedScenarioSince(
                            studentId, shortValue(item.getTarget(), "scenarioId"), since)));
            case ASSIGNMENT -> assignmentSubmissionRepository.findByAssignmentIdAndStudentId(
                    longValue(item.getTarget(), "assignmentId"), studentId).isPresent() ? 1 : 0;
        };
    }

    private int vocabularyCompletedUnits(Long studentId, TodayPlanItem item, LocalDateTime since) {
        long count = vocabularyProgressRepository.countPracticedVocabularyIdsSince(
                studentId, longArray(item.getSourceSnapshot(), "vocabularyIds"), since);
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, count));
    }

    private int roadmapCompletedUnits(TodayPlanItem item, RoadmapProgressResponse progress) {
        if (progress == null || progress.getMilestones() == null) return item.getCompletedUnits();
        String moduleKey = textValue(item.getTarget(), "moduleKey");
        for (RoadmapMilestone milestone : progress.getMilestones()) {
            if (milestone.getModules() == null) continue;
            for (RoadmapModule module : milestone.getModules()) {
                if (moduleKey.equals(module.getModuleKey())) {
                    return module.getDoneCount() == null ? 0 : module.getDoneCount();
                }
            }
        }
        return item.getCompletedUnits();
    }

    private List<Long> longArray(JsonNode root, String field) {
        JsonNode array = required(root, field);
        if (!array.isArray() || array.isEmpty()) {
            throw new IllegalStateException("Today Plan snapshot has no " + field);
        }
        List<Long> values = new ArrayList<>();
        array.forEach(node -> values.add(node.longValue()));
        return values;
    }

    private long longValue(JsonNode root, String field) {
        return required(root, field).longValue();
    }

    private short shortValue(JsonNode root, String field) {
        int value = required(root, field).intValue();
        if (value < 0 || value > Short.MAX_VALUE) {
            throw new IllegalStateException("Today Plan target is outside SMALLINT range: " + field);
        }
        return (short) value;
    }

    private String textValue(JsonNode root, String field) {
        return required(root, field).asText();
    }

    private JsonNode required(JsonNode root, String field) {
        JsonNode value = root == null ? null : root.get(field);
        if (value == null || value.isNull()) {
            throw new IllegalStateException("Today Plan target is missing " + field);
        }
        return value;
    }

    private TodayPlanItemStatus status(int completed, int total) {
        if (completed >= total) return TodayPlanItemStatus.COMPLETED;
        if (completed > 0) return TodayPlanItemStatus.IN_PROGRESS;
        return TodayPlanItemStatus.TODO;
    }

    private <T> T firstNonNull(T value, T fallback) {
        return value == null ? fallback : value;
    }
}
