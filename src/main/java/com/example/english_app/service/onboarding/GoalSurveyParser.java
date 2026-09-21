package com.example.english_app.service.onboarding;

import com.example.english_app.dto.request.GoalSurveyRequest;
import com.example.english_app.entity.enums.TopicCategory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GoalSurveyParser {

    private final ObjectMapper objectMapper;

    public GoalSurveyRequest parse(String goalSurveyJson) {
        if (goalSurveyJson == null || goalSurveyJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(goalSurveyJson, GoalSurveyRequest.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse goal_survey_json", e);
            return null;
        }
    }

    public List<TopicCategory> extractCategories(String goalSurveyJson) {
        GoalSurveyRequest request = parse(goalSurveyJson);
        if (request == null || request.getLearningPurpose() == null) {
            return List.of(TopicCategory.DAILY_CONVERSATION); // fallback
        }

        String purpose = request.getLearningPurpose().trim();

        // Tầng 1: match theo enum key (Flutter có thể gửi "WORK", "TRAVEL", "EXAM_IELTS", v.v.)
        try {
            TopicCategory byKey = TopicCategory.valueOf(purpose.toUpperCase().replace(" ", "_").replace("-", "_"));
            return List.of(byKey);
        } catch (IllegalArgumentException ignored) {
            // Không match enum key → thử text matching bên dưới
        }

        // Tầng 2: match bằng substring tiếng Việt / tiếng Anh (legacy / freeform)
        String lower = purpose.toLowerCase();
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

    public List<String> extractFocusSkills(String goalSurveyJson) {
        GoalSurveyRequest request = parse(goalSurveyJson);
        if (request == null || request.getFocusSkills() == null || request.getFocusSkills().isEmpty()) {
            return Collections.emptyList();
        }
        return request.getFocusSkills();
    }
}
