package com.example.english_app.service.onboarding;

import com.example.english_app.entity.enums.LearnerSkill;
import com.example.english_app.entity.enums.LearningGoal;
import com.example.english_app.entity.enums.TopicCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoalSurveyParser {

    private final ObjectMapper objectMapper;

    public ParsedGoalSurvey parse(String goalSurveyJson) {
        if (goalSurveyJson == null || goalSurveyJson.isBlank()) {
            return defaultSurvey();
        }
        try {
            JsonNode root = objectMapper.readTree(goalSurveyJson);
            String learningGoalCode = text(root, "learningGoal");
            String legacyPurpose = text(root, "learningPurpose");

            LearningGoal learningGoal = parseLearningGoal(learningGoalCode, legacyPurpose);
            List<TopicCategory> categories = learningGoalCode != null
                    ? categoriesForGoal(learningGoal)
                    : legacyCategories(legacyPurpose);
            List<LearnerSkill> focusSkills = parseFocusSkills(root.path("focusSkills"));
            Integer dailyStudyMinutes = integer(root, "dailyStudyMinutes");
            return new ParsedGoalSurvey(learningGoal, categories, focusSkills, dailyStudyMinutes);
        } catch (Exception e) {
            log.warn("Failed to parse goal_survey_json", e);
            return defaultSurvey();
        }
    }

    public List<TopicCategory> extractCategories(String goalSurveyJson) {
        return parse(goalSurveyJson).categories();
    }

    public List<LearnerSkill> extractFocusSkills(String goalSurveyJson) {
        return parse(goalSurveyJson).focusSkills();
    }

    private LearningGoal parseLearningGoal(String code, String legacyPurpose) {
        if (code != null) {
            try {
                return LearningGoal.valueOf(normalizeCode(code));
            } catch (IllegalArgumentException exception) {
                log.warn("Unknown learningGoal code '{}'; using GENERAL", code);
                return LearningGoal.GENERAL;
            }
        }
        List<TopicCategory> categories = legacyCategories(legacyPurpose);
        TopicCategory primary = categories.getFirst();
        return switch (primary) {
            case WORK -> LearningGoal.WORK;
            case TRAVEL -> LearningGoal.TRAVEL;
            case EXAM_IELTS -> LearningGoal.EXAM;
            case DAILY_CONVERSATION -> LearningGoal.COMMUNICATION;
            case STUDY_ABROAD -> LearningGoal.GENERAL;
        };
    }

    private List<TopicCategory> categoriesForGoal(LearningGoal goal) {
        return switch (goal) {
            case WORK -> List.of(TopicCategory.WORK);
            case TRAVEL -> List.of(TopicCategory.TRAVEL);
            case EXAM -> List.of(TopicCategory.EXAM_IELTS);
            case COMMUNICATION, GENERAL -> List.of(TopicCategory.DAILY_CONVERSATION);
        };
    }

    private List<TopicCategory> legacyCategories(String purpose) {
        if (purpose == null || purpose.isBlank()) {
            return List.of(TopicCategory.DAILY_CONVERSATION);
        }

        String normalized = normalizeCode(purpose);
        try {
            TopicCategory byKey = TopicCategory.valueOf(normalized);
            return List.of(byKey);
        } catch (IllegalArgumentException ignored) {
            try {
                return categoriesForGoal(LearningGoal.valueOf(normalized));
            } catch (IllegalArgumentException ignoredGoal) {
                // Fall through to legacy free-text matching.
            }
        }

        String lower = purpose.toLowerCase(Locale.ROOT);
        List<TopicCategory> categories = new ArrayList<>();

        if (lower.contains("công việc") || lower.contains("đi làm") || lower.contains("work")) {
            categories.add(TopicCategory.WORK);
        }
        if (lower.contains("du lịch") || lower.contains("travel")) {
            categories.add(TopicCategory.TRAVEL);
        }
        if (lower.contains("ielts") || lower.contains("thi") || lower.contains("exam")) {
            categories.add(TopicCategory.EXAM_IELTS);
        }
        if (lower.contains("du học") || lower.contains("abroad") || lower.contains("study abroad")) {
            categories.add(TopicCategory.STUDY_ABROAD);
        }
        if (lower.contains("giao tiếp") || lower.contains("hàng ngày") || lower.contains("daily")) {
            categories.add(TopicCategory.DAILY_CONVERSATION);
        }

        // Nếu không match gì → fallback
        if (categories.isEmpty()) {
            log.debug("No category matched for learningPurpose='{}', using DAILY_CONVERSATION fallback", purpose);
            categories.add(TopicCategory.DAILY_CONVERSATION);
        }

        return categories;
    }

    private List<LearnerSkill> parseFocusSkills(JsonNode node) {
        if (!node.isArray()) return List.of();
        Set<LearnerSkill> skills = new LinkedHashSet<>();
        for (JsonNode item : node) {
            if (!item.isTextual()) continue;
            LearnerSkill skill = parseSkill(item.asText());
            if (skill != null) skills.add(skill);
        }
        return List.copyOf(skills);
    }

    private LearnerSkill parseSkill(String value) {
        try {
            return LearnerSkill.valueOf(normalizeCode(value));
        } catch (IllegalArgumentException ignored) {
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.contains("từ vựng") || lower.contains("vocab")) return LearnerSkill.VOCABULARY;
            if (lower.contains("giao tiếp") || lower.contains("speaking") || lower.equals("nói")) {
                return LearnerSkill.SPEAKING;
            }
            if (lower.contains("phát âm") || lower.contains("pronunciation") || lower.contains("ipa")) {
                return LearnerSkill.PRONUNCIATION;
            }
            if (lower.contains("đọc") || lower.contains("reading")) return LearnerSkill.READING;
            if (lower.contains("nghe") || lower.contains("listening")) return LearnerSkill.LISTENING;
            if (lower.contains("ngữ pháp") || lower.contains("grammar")) return LearnerSkill.GRAMMAR;
            log.debug("Ignoring unknown legacy focus skill '{}'", value);
            return null;
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) return null;
        return node.asText().trim();
    }

    private Integer integer(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node != null && node.canConvertToInt() ? node.intValue() : null;
    }

    private String normalizeCode(String value) {
        return value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private ParsedGoalSurvey defaultSurvey() {
        return new ParsedGoalSurvey(
                LearningGoal.GENERAL,
                List.of(TopicCategory.DAILY_CONVERSATION),
                List.of(),
                null);
    }

    public record ParsedGoalSurvey(
            LearningGoal learningGoal,
            List<TopicCategory> categories,
            List<LearnerSkill> focusSkills,
            Integer dailyStudyMinutes) {
    }
}
